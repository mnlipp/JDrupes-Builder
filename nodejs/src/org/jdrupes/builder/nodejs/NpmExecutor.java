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

package org.jdrupes.builder.nodejs;

import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractProvider;

/// A provider for [execution results][ExecResult]s from invoking npm.
/// The provider produces resources in response to requests for
/// [ExecResult]'s where the request's [ResourceRequest#name()] matches
/// this [provider's name][ResourceProvider#name()].
/// 
///   * The provider first checks if a file `package.json` exists, else it
///     fails. If no directory `node_modules` exists or `package.json`
///     is newer than `node_modules/.package-lock.json` it invokes `npm init`.
/// 
///   * Then, the provider retrieves all resources added by [#required]. While
///     the provider itself does not process these resources, it is assumed
///     that they are processed by the npm command and therefore need to be
///     available.
/// 
///   * The provider invokes the function set with [#provided] and
///     collects all resources. If the provided resources exist and no
///     resource from `required` is newer then the provided resources found,
///     the provider returns a result that indicates successful invocation.
///     The date of the result is set to the newest date from the provided
///     resources and the (existing) resources are attached.  
/// 
///   * Else, the provider invokes npm, calls the function set with
///     `provided` again and adds the result to the [ExecResult] that
///     it returns.
///
/// The provider also uses the function set with [#provided] to determine
/// the resources to be removed when it is invoked with a request for
/// [Cleanliness].
///
public class NpmExecutor extends AbstractProvider implements Renamable {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final Project project;
    private final List<String> arguments = new ArrayList<>();
    private final List<Stream<Resource>> requiredResources = new ArrayList<>();
    private Function<Project, Stream<Resource>> getProvided
        = _ -> Stream.empty();

    /// Initializes a new NPM executor.
    ///
    /// @param project the project
    ///
    public NpmExecutor(Project project) {
        this.project = project;
        rename(NpmExecutor.class.getSimpleName() + " in " + project);
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

    /// Add the given arguments.
    ///
    /// @param args the arguments
    /// @return the npm executor
    ///
    public NpmExecutor args(String... args) {
        arguments.addAll(Arrays.asList(args));
        return this;
    }

    /// Add the given [Stream] of resources to the required resources.
    ///
    /// @param resources the resources
    /// @return the npm executor
    ///
    public NpmExecutor required(Stream<Resource> resources) {
        requiredResources.add(resources);
        return this;
    }

    /// Convenience method to add a [FileTree] to the required resources.
    /// If `root` is a relative path, it is resolved against the project's
    /// directory. 
    ///
    /// @param root the root
    /// @param pattern the pattern
    /// @return the npm executor
    ///
    public NpmExecutor required(Path root, String pattern) {
        requiredResources
            .add(Stream.of(FileTree.create(project, root, pattern)));
        return this;
    }

    /// Convenience method to add a [FileResource] to the required resources.
    /// If `path` is relative, it is resolved against the project's directory. 
    ///
    /// @param root the root
    /// @return the npm executor
    ///
    public NpmExecutor required(Path root) {
        requiredResources.add(Stream.of(FileResource.create(project, root)));
        return this;
    }

    /// Sets the function used to determine the resources provided by this
    /// provider.
    ///
    /// @param resources the resources
    /// @return the npm executor
    ///
    public NpmExecutor provided(Function<Project, Stream<Resource>> resources) {
        this.getProvided = resources;
        return this;
    }

    @Override
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (requested.accepts(CleanlinessType)) {
            getProvided.apply(project).forEach(Resource::cleanup);
            return Stream.empty();
        }
        if (!requested.accepts(ExecResultType)
            || requested.name().map(n -> !n.equals(name())).orElse(false)) {
            return Stream.empty();
        }

        // Check prerequisites
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

        // Make sure that the required resources exists
        var required = newResource(new ResourceType<Resources<Resource>>() {});
        requiredResources.stream().forEach(required::addAll);

        // Get (previously) provided and check if up-to-date
        var provided = newResource(new ResourceType<Resources<Resource>>() {});
        provided.addAll(getProvided.apply(project));
        if (required.asOf().isPresent() && provided.asOf().isPresent()
            && !required.asOf().get().isAfter(provided.asOf().get())) {
            var execResult = newResource(ExecResultType, this,
                "existing " + provided.stream().map(Resource::toString)
                    .collect(Collectors.joining(", ")),
                0, provided.stream());
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) Stream.of(execResult);
            return result;

        }

        return runNpm(project, arguments);
    }

    private <T extends Resource> Stream<T> runNpm(
            Project project, List<String> arguments) {
        List<String> command = new ArrayList<>(List.of("npm"));
        command.addAll(arguments);
        ProcessBuilder processBuilder = new ProcessBuilder(command)
            .directory(project.directory().toFile())
            .redirectInput(Redirect.INHERIT);
        try {
            Process process = processBuilder.start();
            copyData(process.getInputStream(), context().out());
            copyData(process.getErrorStream(), context().error());
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) Stream.of(newResource(ExecResultType, this,
                "[" + project.name() + "]$ "
                    + command.stream().collect(Collectors.joining(" ")),
                process.waitFor(), getProvided.apply(project)));
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
}
