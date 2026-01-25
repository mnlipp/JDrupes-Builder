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
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ExecResult;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Renamable;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceRetriever;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractProvider;
import org.jdrupes.builder.core.StreamCollector;
import static org.jdrupes.builder.java.JavaTypes.*;

/// A provider for [execution results][ExecResult]s from invoking a JVM.
///
public class JavaExecutor extends AbstractProvider
        implements ResourceRetriever, Renamable {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final StreamCollector<ResourceProvider> providers
        = StreamCollector.cached();
    private String mainClass;

    /// Initializes a new java executor.
    ///
    /// @param project the project
    ///
    public JavaExecutor(Project project) {
        rename(JavaExecutor.class.getSimpleName() + " in " + project);
    }

    @Override
    public JavaExecutor name(String name) {
        rename(name);
        return this;
    }

    /// Additionally uses the given providers for obtaining contents for the
    /// jar.
    ///
    /// @param providers the providers
    /// @return the jar generator
    ///
    @Override
    public JavaExecutor addFrom(ResourceProvider... providers) {
        addFrom(Stream.of(providers));
        return this;
    }

    /// Additionally uses the given providers for obtaining contents for the
    /// jar.
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

    @Override
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.accepts(ExecResultType)
            || requested.name().map(n -> !n.equals(name())).orElse(false)) {
            return Stream.empty();
        }

        // Collect the classpath and check mainClass.
        var cpResources = newResource(ClasspathType)
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
            throw new BuildException("No main class defined for " + name());
        }

        // Build command
        List<String> command = List.of(
            System.getProperty("java.home") + "/bin/java",
            "-cp", cpResources.stream().map(e -> e.toPath().toString())
                .collect(Collectors.joining(File.pathSeparator)),
            mainClass);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();
        try {
            Process process = processBuilder.start();
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) Stream.of(newResource(ExecResultType, this,
                mainClass, process.waitFor()));
            return result;
        } catch (IOException | InterruptedException e) {
            throw new BuildException(e);
        }
    }

    private void findMainClass(Resources<ClasspathElement> cpResources) {
        cpResources.stream().filter(cpe -> cpe instanceof JarFile)
            .map(JarFile.class::cast)
            .mapMulti((JarFile cpe, Consumer<String> consumer) -> {
                try (var jar
                    = new java.util.jar.JarFile(cpe.path().toFile())) {
                    Optional.of(jar).map(t -> {
                        try {
                            return t.getManifest();
                        } catch (IOException e) {
                            return null;
                        }
                    }).map(Manifest::getMainAttributes)
                        .map(a -> a.getValue(Attributes.Name.MAIN_CLASS))
                        .ifPresent(consumer::accept);
                } catch (IOException e) {
                    logger.atWarning().withCause(e).log("Problem reading %s",
                        cpe);
                }
            }).findFirst().ifPresent(mc -> mainClass = mc);
    }

}
