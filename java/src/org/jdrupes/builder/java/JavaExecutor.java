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

package org.jdrupes.builder.java;

import com.google.common.flogger.FluentLogger;
import static com.google.common.flogger.LazyArgs.*;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
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
import org.jdrupes.builder.api.ResourceRetriever;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractProvider;
import org.jdrupes.builder.core.StreamCollector;
import static org.jdrupes.builder.java.JavaTypes.*;

/// A provider for [execution results][ExecResult]s from invoking a JVM.
///
public class JavaExecutor extends AbstractProvider
        implements ResourceRetriever, Renamable, RequiredResourceSupport {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final Project project;
    private final StreamCollector<Resource> requiredResources
        = new StreamCollector<>(false);
    private final StreamCollector<ResourceProvider> providers
        = StreamCollector.cached();
    private String mainClass;
    private final List<String> arguments = new ArrayList<>();

    /// Initializes a new java executor.
    ///
    /// @param project the project
    ///
    public JavaExecutor(Project project) {
        this.project = project;
        rename(JavaExecutor.class.getSimpleName() + " in " + project);
    }

    @Override
    public JavaExecutor name(String name) {
        rename(name);
        return this;
    }

    @Override
    public JavaExecutor required(Stream<? extends Resource> resources) {
        requiredResources.add(resources);
        return this;
    }

    @Override
    public JavaExecutor required(Path root, String pattern) {
        requiredResources
            .add(Stream.of(FileTree.of(project, root, pattern)));
        return this;
    }

    @Override
    public JavaExecutor required(Path root) {
        requiredResources.add(
            Stream.of(FileResource.of(project.directory().resolve(root))));
        return this;
    }

    /// Additionally uses the given providers for building the classpath.
    ///
    /// @param providers the providers
    /// @return the jar generator
    ///
    @Override
    public JavaExecutor addFrom(ResourceProvider... providers) {
        addFrom(Stream.of(providers));
        return this;
    }

    /// Additionally uses the given providers for building the classpath.
    ///
    /// @param providers the providers
    /// @return the jar generator
    ///
    @Override
    public JavaExecutor addFrom(Stream<ResourceProvider> providers) {
        this.providers.add(providers.filter(p -> !p.equals(this)));
        return this;
    }

    /// Returns the main class.
    ///
    /// @return the main class
    ///
    public String mainClass() {
        return mainClass;
    }

    /// Sets the main class.
    ///
    /// @param mainClass the new main class
    /// @return the jar generator for method chaining
    ///
    public JavaExecutor mainClass(String mainClass) {
        this.mainClass = Objects.requireNonNull(mainClass);
        return this;
    }

    /// Add the given arguments.
    ///
    /// @param args the arguments
    /// @return the npm executor
    ///
    public JavaExecutor args(String... args) {
        arguments.addAll(Arrays.asList(args));
        return this;
    }

    @Override
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.accepts(ExecResultType)
            || requested.name().map(n -> !n.equals(name())).orElse(false)) {
            return Stream.empty();
        }

        // Make sure that the required resources are retrieved and exist
        var required = Resources.of(new ResourceType<Resources<Resource>>() {});
        required.addAll(requiredResources.stream());

        // Collect the classpath and check mainClass.
        var cpResources = Resources.of(ClasspathType)
            .addAll(providers.stream()
                .map(p -> p.resources(of(ClasspathElementType).usingAll()))
                .flatMap(s -> s));
        logger.atFiner().log("Executing with classpath %s",
            lazy(() -> cpResources.stream().map(e -> e.toPath().toString())
                .collect(Collectors.joining(File.pathSeparator))));
        if (mainClass == null) {
            findMainClass(cpResources);
        }
        if (mainClass == null) {
            throw new ConfigurationException().from(this).message(
                "No main class defined for %s", name());
        }

        // Build command
        List<String> command = new ArrayList<>(List.of(
            System.getProperty("java.home") + "/bin/java",
            "-cp", cpResources.stream().map(e -> e.toPath().toString())
                .collect(Collectors.joining(File.pathSeparator)),
            mainClass));
        command.addAll(arguments);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectInput(Redirect.INHERIT);
        try {
            Process process = processBuilder.start();
            copyData(process.getInputStream(), context().out());
            copyData(process.getErrorStream(), context().error());
            var execResult
                = ExecResult.of(this, mainClass, process.waitFor());
            if (execResult.exitValue() != 0) {
                execResult.setFaulty();
            }
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) Stream.of(execResult);
            return result;
        } catch (IOException | InterruptedException e) {
            throw new BuildException().from(this).cause(e);
        }
    }

    private void findMainClass(Resources<ClasspathElement> cpResources) {
        vavrStream(cpResources).filter(cpe -> cpe instanceof JarFile)
            .map(JarFile.class::cast).map(cpe -> Try.withResources(
                () -> new java.util.jar.JarFile(cpe.path().toFile()))
                .of(jar -> Try.of(jar::getManifest).toOption()
                    .flatMap(Option::of).map(Manifest::getMainAttributes)
                    .flatMap(a -> Option
                        .of(a.getValue(Attributes.Name.MAIN_CLASS))))
                .onFailure(e -> logger.atWarning().withCause(e).log(
                    "Problem reading %s", cpe))
                .toOption().flatMap(s -> s))
            .flatMap(Option::toStream).headOption().peek(mc -> mainClass = mc);
    }

    private void copyData(InputStream source, OutputStream sink) {
        Thread.startVirtualThread(() -> {
            try (source) {
                source.transferTo(sink);
            } catch (IOException e) { // NOPMD
            }
        });
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        return super.toString() + "[" + project.name() + "]";
    }
}
