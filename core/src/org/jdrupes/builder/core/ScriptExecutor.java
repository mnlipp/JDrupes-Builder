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

package org.jdrupes.builder.core;

import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.ConfigurationException;
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

/// A provider of [execution results][ExecResult] from invoking a script
/// executed by a configurable [interpreter][#interpreter(Path)].
/// The provider generates resources in response to requests for
/// [ExecResult] whose [name][ResourceRequest#name()] matches this
/// [provider's name][ResourceProvider#name()].
/// 
///   * The working directory is the project directory.
/// 
///   * The provider retrieves all resources added by [#required]. While
///     the provider itself does not process these resources, it is assumed
///     that they are processed by the script and therefore need to be
///     available.
/// 
///   * The provider invokes the function configured with [#output] and
///     collects all resources. If the generated resources exist and no
///     resource from `required` is newer than the generated resources found,
///     the provider returns a result that indicates successful invocation.
///     The date of the result is set to the newest date from the generated
///     resources and the (existing) resources are attached.  
/// 
///   * Else, the provider executes the script, calls the function set with
///     [#output] again and adds the result to the [ExecResult] that
///     it returns.
/// 
/// The generated resources can also be provided directly (i.e. not as part
/// of an [ExecResult]) in response to a configurable resource request, see
/// [#provideResources(ResourceRequest, Function)].
///
/// The provider also uses the function set with [#output] to determine
/// the resources to be removed when it is invoked with a request for
/// [Cleanliness].
///
public class ScriptExecutor extends AbstractProvider
        implements Renamable, RequiredResourceSupport {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final Project project;
    private final List<String> interpreterFlags = new ArrayList<>();
    private Path interpreter = Path.of("/usr/bin/bash");
    private Path scriptFile;
    private String scriptArgumentFlag = "-c";
    private String script;
    private final StreamCollector<String> arguments
        = new StreamCollector<>(true);
    private final StreamCollector<Resource> requiredResources
        = new StreamCollector<>(true);
    private Function<Project, Stream<Resource>> getOutput
        = _ -> Stream.empty();
    private ResourceRequest<?> requestForGenerated;

    /// Initializes a new script executor.
    ///
    /// @param project the project
    ///
    public ScriptExecutor(Project project) {
        this.project = project;
        rename(ScriptExecutor.class.getSimpleName() + " in " + project);
    }

    /// Returns the project that this provider belongs to.
    ///
    /// @return the project
    ///
    public Project project() {
        return project;
    }

    @Override
    public ScriptExecutor name(String name) {
        rename(name);
        return this;
    }

    /// Sets the path to the interpreter that is to be invoked.
    ///
    /// @param interpreter the interpreter
    /// @return the script executor
    ///
    public ScriptExecutor interpreter(Path interpreter) {
        this.interpreter = interpreter;
        return this;
    }

    /// Adds the given flags to the interpreter invocation.
    ///
    /// @param flags the flags
    /// @return the script executor
    ///
    public ScriptExecutor interpreterFlags(String... flags) {
        interpreterFlags.addAll(Arrays.asList(flags));
        return this;
    }

    /// Sets the script file to be executed.
    ///
    /// @param script the script
    /// @return the script executor
    ///
    public ScriptExecutor scriptFile(Path script) {
        if (this.script != null) {
            throw new ConfigurationException().from(this).message(
                "Either script or scriptFile may be set");
        }
        this.scriptFile = project().directory().resolve(script);
        return this;
    }

    /// Sets the flag that precedes a script passed to the interpreter
    /// on the command line. Defaults to "-c".
    ///
    /// @param flag the flag
    /// @return the script executor
    ///
    public ScriptExecutor scriptArgumentFlag(String flag) {
        scriptArgumentFlag = flag;
        return this;
    }

    /// Sets the script to be executed.
    ///
    /// @param script the script
    /// @return the script executor
    ///
    public ScriptExecutor script(String script) {
        if (scriptFile != null) {
            throw new ConfigurationException().from(this).message(
                "Either script or scriptFile may be set");
        }
        this.script = script;
        return this;
    }

    /// Add the given arguments as arguments of the script. Note that
    /// the absolute path of the script or, when using [#script(String)],
    /// this provider's name is automatically added as the first
    /// argument, before the arguments specified by this method.
    ///
    /// @param args the arguments
    /// @return the script executor
    ///
    public ScriptExecutor args(String... args) {
        arguments.add(Arrays.asList(args).stream());
        return this;
    }

    /// Add the strings from the stream as arguments of the script,
    /// see [#args(String...)].
    ///
    /// @param args the args
    /// @return the script executor
    ///
    public ScriptExecutor args(Stream<String> args) {
        arguments.add(args);
        return this;
    }

    /// Required.
    ///
    /// @param resources the resources
    /// @return the script executor
    ///
    @Override
    public ScriptExecutor required(Stream<? extends Resource> resources) {
        requiredResources.add(resources);
        return this;
    }

    @Override
    public ScriptExecutor required(Path root, String pattern) {
        requiredResources
            .add(Stream.of(FileTree.of(project, root, pattern)));
        return this;
    }

    @Override
    public ScriptExecutor required(Path file) {
        requiredResources.add(
            Stream.of(FileResource.of(project.directory().resolve(file))));
        return this;
    }

    /// Sets the function used to determine the resources generated by this
    /// provider. The function is evaluated both for incremental
    /// up-to-date checks, for determining the resources returned
    /// after script execution, and for determining the resources to be
    /// cleaned.
    ///
    /// @param resources the function that provides the results as resources
    /// @return the script executor
    ///
    public ScriptExecutor output(
            Function<Project, Stream<Resource>> resources) {
        this.getOutput = resources;
        return this;
    }

    /// Provide the generated resources directly in response to a requests
    /// like the given prototype request. Invoking this method implies a
    /// call to [#output(Function)] with `resources`.
    ///
    /// @param proto defines the kind of request that the script executor
    /// should respond to with the generated resources
    /// @param resources the function that provides the results as resources
    /// @return the script executor
    ///
    public ScriptExecutor provideResources(ResourceRequest<?> proto,
            Function<Project, Stream<Resource>> resources) {
        requestForGenerated = proto;
        return output(resources);
    }

    @Override
    protected <T extends Resource> Collection<T>
            doProvide(ResourceRequest<T> request) {
        if (request.accepts(CleanlinessType)) {
            getOutput.apply(project).forEach(Resource::cleanup);
            return Collections.emptyList();
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
            return Collections.emptyList();
        }
        // Always evaluate for the most special type
        if (!request.type().equals(ExecResultType)) {
            @SuppressWarnings({ "unchecked", "PMD.AvoidDuplicateLiterals" })
            var result = (Collection<T>) resources(of(ExecResultType)
                .withName(name())).toList();
            return result;
        }

        // Make sure that the required resources are retrieved and exist
        var required = Resources.of(new ResourceType<Resources<Resource>>() {});
        required.addAll(requiredResources.stream());

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
            return List.of(result);
        }
        return runScript(project);
    }

    private <T extends Resource> Collection<T> runScript(Project project) {
        logger.atFine().log("Running %s with %s", this, interpreter);
        List<String> command
            = new ArrayList<>(List.of(interpreter.toString()));
        command.addAll(interpreterFlags);
        if (scriptFile != null) {
            command.add(scriptFile.toString());
        }
        if (script != null) {
            command.add(scriptArgumentFlag);
            command.add(script);
            command.add(name());
        }
        arguments.stream().forEach(command::add);
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
                    .message("Interpreter exited with %d", exitValue);
            }
            @SuppressWarnings("unchecked")
            var result = (Collection<T>) List.of(ExecResult.of(this,
                "[" + project.name() + "]$ ... "
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
            } catch (IOException e) {
                throw new BuildException().from(this).cause(e);
            }
        });
    }

    private <T extends Resource> Collection<T> provideGenerated() {
        // Request execution result
        var execResult = resources(of(ExecResultType).withName(name()));
        @SuppressWarnings("unchecked")

        // Extract generated
        var generated = execResult.map(r -> (Stream<T>) r.resources())
            .flatMap(s -> s).toList();
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
