/*
 * JDrupes Builder
 * Copyright (C) 2025 Michael N. Lipp
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

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractGenerator;
import static org.jdrupes.builder.java.JavaConsts.*;

/// The [JavaCompiler] generator provides two types of resources.
/// 
/// 1. The [JavaSourceFile]s of the project as configured with [addSources]
///    in response to a [ResourceRequest] with [ResourceType]
///    [JavaConsts#JAVA_SOURCE_FILES].
///
/// 2. The [ClassFile]s that result from compiling the sources in response
///    to a [ResourceRequest] with [ResourceType]
///    [JavaConsts#JAVA_CLASS_FILES].
///
public class JavaCompiler extends AbstractGenerator<FileTree<ClassFile>> {

    private final Resources<FileTree<JavaSourceFile>> sources
        = project().newResources(FileResource.class);

    /// Instantiates a new java compiler.
    ///
    /// @param project the project
    ///
    public JavaCompiler(Project project) {
        super(project);
    }

    /// Adds the source tree.
    ///
    /// @param sources the sources
    /// @return the java compiler
    ///
    public final JavaCompiler addSources(FileTree<JavaSourceFile> sources) {
        this.sources.add(sources);
        return this;
    }

    /// Adds the files from the given directory matching the given pattern.
    /// Short for
    /// `addSources(project().newFileTree(directory, pattern, JavaSourceFile.class))`.
    ///
    /// @param directory the directory
    /// @param pattern the pattern
    /// @return the resources collector
    ///
    public final JavaCompiler addSources(Path directory, String pattern) {
        addSources(
            project().newFileTree(directory, pattern, JavaSourceFile.class));
        return this;
    }

    /// Adds the sources.
    ///
    /// @param sources the sources
    /// @return the java compiler
    ///
    public final JavaCompiler
            addSources(Stream<FileTree<JavaSourceFile>> sources) {
        this.sources.addAll(sources);
        return this;
    }

    /// Source paths.
    ///
    /// @return the collection
    ///
    public Collection<Path> sourcePaths() {
        return sources.stream().map(Resources::stream)
            .flatMap(Function.identity()).map(FileResource::path)
            .collect(Collectors.toList());
    }

    @Override
    public <T extends Resource> Stream<T>
            provide(ResourceRequest<T> requested) {
        if (requested.type().isAssignableFrom(JAVA_SOURCE_FILES)) {
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) sources.stream();
            return result;
        }
        if (!requested.type().isAssignableFrom(JAVA_CLASS_FILES)) {
            return Stream.empty();
        }

        // Get this project's previously generated classes (for checking)
        var destDir = project().buildDirectory().resolve("classes");
        final var classSet = project().newFileTree(
            destDir, "**/*", ClassFile.class);

        // Get classpath for compilation.
        log.fine(() -> "Getting classpath for " + project());
        var cpResources = project().newResources(ClassFile.class).addAll(
            project().provided(EnumSet.of(Intend.Consume, Intend.Expose),
                new ResourceRequest<>(JAVA_CLASS_FILES)));
        log.finest(() -> project() + " uses classpath: " + cpResources.stream()
            .map(Resource::toString).collect(Collectors.joining(", ")));

        // (Re-)compile only if necessary
        var classesAsOf = classSet.asOf();
        if (sources.asOf().isAfter(classesAsOf)
            || cpResources.asOf().isAfter(classesAsOf)
            || classSet.stream().count() < sources.stream()
                .flatMap(Resources::stream).map(FileResource::path)
                .filter(p -> p.toString().endsWith(".java")
                    && !p.endsWith("package-info.java")
                    && !p.endsWith("module-info.java"))
                .count()) {
            classSet.delete();
            compile(cpResources, destDir);
        }
        classSet.clear();
        @SuppressWarnings("unchecked")
        var result = (Stream<T>) Stream.of(classSet);
        return result;
    }

    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.ExceptionAsFlowControl" })
    private void compile(Resources<Resource> cpResources, Path destDir) {
        log.info(() -> "Compiling Java in project " + project().name());
        var classpath = cpResources.stream().<Path> mapMulti((r, sink) -> {
            if (r instanceof FileTree fileSet) {
                sink.accept(fileSet.root());
            }
        }).map(Path::toString).collect(Collectors.joining(File.pathSeparator));
        var javac = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var fileManager
            = javac.getStandardFileManager(diagnostics, null, null)) {
            var compilationUnits
                = fileManager.getJavaFileObjectsFromPaths(sourcePaths());
            if (!javac.getTask(null, fileManager, null,
                List.of("-d", destDir.toString(),
                    "-cp", classpath),
                null, compilationUnits).call()) {
                throw new BuildException("Compilation failed");
            }
        } catch (Exception e) {
            log.log(java.util.logging.Level.SEVERE, () -> "Project "
                + project().name() + ": " + "Problem compiling Java: "
                + e.getMessage());
            throw new BuildException(e);
        } finally {
            for (var diagnostic : diagnostics.getDiagnostics()) {
                log.info(() -> String.format("Error on line %d in %s%n",
                    diagnostic.getLineNumber(),
                    diagnostic.getSource().toUri()));
            }
        }
    }

}
