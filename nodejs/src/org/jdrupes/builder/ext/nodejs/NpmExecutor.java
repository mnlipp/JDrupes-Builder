/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jdrupes.builder.ext.nodejs;

import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.ExecResult;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Renamable;
import org.jdrupes.builder.api.RequiredResourceSupport;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractProvider;
import org.jdrupes.builder.core.StreamCollector;

/// A provider for [execution results][ExecResult] from invoking npm.
/// The provider generates resources in response to requests for
/// [ExecResult] where the request's [ResourceRequest#name()] matches
/// this [provider's name][ResourceProvider#name()].
/// 
///   * The working directory is the project directory.
/// 
///   * The provider first checks if a file `package.json` exists, else it
///     fails. If no directory `node_modules` exists or `package.json`
///     is newer than `node_modules/.package-lock.json` it invokes `npm init`.
/// 
///   * Then, the provider retrieves all resources added by [#required]. While
///     the provider itself does not process these resources, it is assumed
///     that they are processed by the `npm` command and therefore need to be
///     available.
/// 
///   * If no arguments were specified, the provider returns an [ExecResult]
///     that indicates successful invocation. The date of the result is set
///     to the date of `node_modules/.package-lock.json`.
/// 
///   * The provider invokes the function configured with [#output] and
///     collects all resources. If the generated resources exist and no
///     resource from `required` is newer then the generated resources found,
///     the provider returns a result that indicates successful invocation.
///     The date of the result is set to the newest date from the generated
///     resources and the (existing) resources are attached.  
/// 
///   * Else, the provider invokes npm, calls the function set with
///     `provided` again and adds the result to the [ExecResult] that
///     it returns.
///
/// The provider also uses the function set with [#output] to determine
/// the resources to be removed when it is invoked with a request for
/// [Cleanliness].
/// 
/// The generated resources can also be provided directly in response
/// to a request, see [#provideResources(ResourceRequest)].
/// 
/// This provider is made available as an extension.
/// [![org.jdrupes:jdbld-ext-nodejs:](
/// https://img.shields.io/maven-central/v/org.jdrupes/jdbld-ext-nodejs?label=org.jdrupes:jdbld-ext-nodejs%3A)
/// ](https://mvnrepository.com/artifact/org.jdrupes/jdbld-ext-nodejs)
///
public class NpmExecutor extends AbstractProvider
        implements Renamable, RequiredResourceSupport {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final Project project;
    private final List<String> arguments = new ArrayList<>();
    private final StreamCollector<Resource> requiredResources
        = new StreamCollector<>(false);
    private Function<Project, Stream<Resource>> getOutput
        = _ -> Stream.empty();
    private String nodeJsVersion;
    private NodeJsDownloader nodeJsDownloader;
    private ResourceRequest<?> requestForGenerated;

    /// Initializes a new NPM executor.
    ///
    /// @param project the project
    ///
    public NpmExecutor(Project project) {
        this.project = project;
        rename(NpmExecutor.class.getSimpleName() + " in " + project);
    }

    /// Returns the project that this provider belongs to.
    ///
    /// @return the project
    ///
    public Project project() {
        return project;
    }

    /// Name.
    ///
    /// @param name the name
    /// @return the npm executor
    ///
    @Override
    public NpmExecutor name(String name) {
        rename(name);
        return this;
    }

    /// Sets the node.js version to use. Setting a version is mandatory.
    ///
    /// @param version the version
    /// @return the npm executor
    ///
    public NpmExecutor nodeJsVersion(String version) {
        nodeJsVersion = version;
        return this;
    }

    /// Add the given arguments.
    ///
    /// @param args the arguments
    /// @return the npm executor
    ///
    public NpmExecutor args(String... args) {
        arguments.addAll(Arrays.asList(args));
        return this;
    }

    @Override
    public NpmExecutor required(Stream<? extends Resource> resources) {
        requiredResources.add(resources);
        return this;
    }

    @Override
    public NpmExecutor required(Path root, String pattern) {
        requiredResources
            .add(Stream.of(FileTree.of(project, root, pattern)));
        return this;
    }

    @Override
    public NpmExecutor required(Path root) {
        requiredResources.add(
            Stream.of(FileResource.of(project.directory().resolve(root))));
        return this;
    }

    /// Sets the function used to determine the resources generated by this
    /// provider.
    ///
    /// @param resources the resources
    /// @return the npm executor
    ///
    public NpmExecutor output(
            Function<Project, Stream<Resource>> resources) {
        this.getOutput = resources;
        return this;
    }

    /// Provide the generated resources in response to a request like
    /// the given one.
    ///
    /// @param proto defines the kind of request that the npm executor
    /// should respond to with the generated resources
    /// @return the npm executor
    ///
    public NpmExecutor provideResources(ResourceRequest<?> proto) {
        requestForGenerated = proto;
        return this;
    }

    @Override
    @SuppressWarnings({ "PMD.CyclomaticComplexity" })
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> request) {
        if (request.accepts(CleanlinessType)) {
            getOutput.apply(project).forEach(Resource::cleanup);
            return Stream.empty();
        }

        // Handle request for generated resources
        if (requestForGenerated != null
            && request.accepts(requestForGenerated.type())
            && (requestForGenerated.name().isEmpty()
                || Objects.equals(requestForGenerated.name().get(),
                    request.name().orElse(null)))) {
            // No need to evaluate for most special type because
            // everything is derived from the exec result
            return provideGenerated();
        }

        // Check for and handle request for execution result
        if (!request.accepts(ExecResultType)
            || !name().equals(request.name().orElse(null))) {
            return Stream.empty();
        }
        // Always evaluate for the most special type
        if (!request.type().equals(ExecResultType)) {
            @SuppressWarnings({ "unchecked", "PMD.AvoidDuplicateLiterals" })
            var result = (Stream<T>) resources(of(ExecResultType)
                .withName(name()));
            return result;
        }

        // Check prerequisites
        if (nodeJsVersion == null) {
            throw new BuildException().from(this)
                .message("No node.js version specified");
        }
        nodeJsDownloader = new NodeJsDownloader(this, context()
            .commonCacheDirectory().resolve(getClass().getPackageName()));
        File packageJson = project.directory().resolve("package.json").toFile();
        if (!packageJson.canRead()) {
            throw new BuildException().from(this)
                .message("No package.json in %s", project);
        }
        File dotPackageLock = project.directory()
            .resolve("node_modules/.package-lock.json").toFile();
        if (!project.directory().resolve("node_modules").toFile().exists()
            || !dotPackageLock.exists()
            || packageJson.lastModified() > dotPackageLock.lastModified()) {
            logger.atConfig().log("Updating node_modules in %s", project);
            runNpm(project, List.of("install"));
        }

        // Make sure that the required resources are retrieved and exist
        var required = Resources.of(new ResourceType<Resources<Resource>>() {});
        required.addAll(requiredResources.stream());
        if (arguments.isEmpty()) {
            @SuppressWarnings("unchecked")
            var result = (T) ExecResult
                .of(this, "npm install", 0, Stream.empty())
                .asOf(Instant.ofEpochMilli(dotPackageLock.lastModified()));
            return Stream.of(result);
        }

        // Get (previously) provided and check if up-to-date
        var existing = Resources.of(new ResourceType<Resources<Resource>>() {});
        existing.addAll(getOutput.apply(project));
        if (required.asOf().isPresent() && existing.asOf().isPresent()
            && !required.asOf().get().isAfter(existing.asOf().get())) {
            logger.atFine().log("Output from %s is up to date", this);
            @SuppressWarnings("unchecked")
            var result = (T) ExecResult.of(this,
                "existing " + existing.stream().map(Resource::toString)
                    .collect(Collectors.joining(", ")),
                0, existing.stream()).asOf(existing.asOf().get());
            return Stream.of(result);
        }
        return runNpm(project, arguments);
    }

    private <T extends Resource> Stream<T> runNpm(
            Project project, List<String> arguments) {
        var nodeJsExecutable = nodeJsDownloader.npmExecutable(nodeJsVersion);
        logger.atFine().log("Running %s with %s", this, nodeJsExecutable);
        List<String> command
            = new ArrayList<>(List.of(nodeJsExecutable.toString()));
        command.addAll(arguments);
        ProcessBuilder processBuilder = new ProcessBuilder(command)
            .directory(project.directory().toFile())
            .redirectInput(Redirect.INHERIT);
        try {
            Process process = processBuilder.start();
            copyData(process.getInputStream(), context().out());
            copyData(process.getErrorStream(), context().error());
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                throw new BuildException().from(this)
                    .message("Npm exited with %d", exitValue);
            }
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) Stream.of(ExecResult.of(this,
                "[" + project.name() + "]$ npm "
                    + arguments.stream().collect(Collectors.joining(" ")),
                exitValue, getOutput.apply(project))
                .asOf(Instant.now()));
            return result;
        } catch (IOException | InterruptedException e) {
            throw new BuildException().from(this).cause(e);
        }
    }

    private void copyData(InputStream source, OutputStream sink) {
        Thread.startVirtualThread(() -> {
            try (source) {
                source.transferTo(sink);
            } catch (IOException e) { // NOPMD
            }
        });
    }

    private <T extends Resource> Stream<T> provideGenerated() {
        // Request execution result
        var execResult = resources(of(ExecResultType).withName(name()));
        @SuppressWarnings("unchecked")

        // Extract generated
        var generated = execResult.map(r -> (Stream<T>) r.resources())
            .flatMap(s -> s);
        return generated;
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        return super.toString() + "[" + project().name() + "]";
    }
}
