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
import java.util.ArrayList;
import java.util.Collection;
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
import org.jdrupes.builder.api.Project;
import static org.jdrupes.builder.api.Project.Properties.*;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import static org.jdrupes.builder.java.JavaTypes.*;

/// The [JavaCompiler] generator provides two types of resources.
/// 
/// 1. The [JavaSourceFile]s of the project as configured with [addSources]
///    in response to a [ResourceRequest] with [ResourceType]
///    [JavaTypes#JavaSourceTreeType] (or a more general type).
///
/// 2. The [ClassFile]s that result from compiling the sources in response
///    to a [ResourceRequest] with [ResourceType]
///    [JavaTypes#ClassTreeType] (or a more general type such as
///    [JavaTypes#ClasspathElementType]).
///
public class JavaCompiler extends JavaTool {

    private final Resources<FileTree<JavaSourceFile>> sources
        = project().newResource(new ResourceType<>() {});
    private Path destination = Path.of("classes");

    /// Instantiates a new java compiler.
    ///
    /// @param project the project
    ///
    public JavaCompiler(Project project) {
        super(project);
    }

    /// Returns the destination directory. Defaults to "`classes`".
    ///
    /// @return the destination
    ///
    public Path destination() {
        return project().buildDirectory().resolve(destination);
    }

    /// Sets the destination directory. The [Path] is resolved against
    /// the project's build directory (see [Project#buildDirectory]).
    ///
    /// @param destination the new destination
    /// @return the java compiler
    ///
    public JavaCompiler destination(Path destination) {
        this.destination = destination;
        return this;
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
            project().newResource(JavaSourceTreeType, directory, pattern));
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

    /// Return the source trees configured for the compiler.
    ///
    /// @return the resources
    ///
    public Resources<FileTree<JavaSourceFile>> sources() {
        return sources;
    }

    /// Source paths.
    ///
    /// @return the collection
    ///
    private Collection<Path> sourcePaths() {
        return sources.stream().map(Resources::stream)
            .flatMap(Function.identity()).map(FileResource::path)
            .collect(Collectors.toList());
    }

    @Override
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (requested.includes(JavaSourceTreeType)) {
            @SuppressWarnings({ "unchecked" })
            var result = (Stream<T>) sources.stream();
            return result;
        }

        if (!requested.includes(ClassTreeType)
            && !requested.includes(CleanlinessType)) {
            return Stream.empty();
        }

        // Map special requests ([RuntimeResources], [CompilationResources])
        // to the base request
        if (!ClasspathType.rawType().equals(requested.type().rawType())) {
            return project().from(this)
                .get(requested.widened(ClasspathType.rawType()));
        }

        // Get this project's previously generated classes for checking
        // or deleting.
        var destDir = project().buildDirectory().resolve(destination);
        final var classSet = project().newResource(ClassTreeType, destDir);
        if (requested.includes(CleanlinessType)) {
            classSet.delete();
            return Stream.empty();
        }

        // Get classpath for compilation.
        @SuppressWarnings("PMD.UseDiamondOperator")
        var cpResources = project().newResource(ClasspathType).addAll(
            project().provided(new ResourceRequest<ClasspathElement>(
                CompilationResourcesType)));
        log.finest(() -> "Compiling in " + project() + " with classpath "
            + cpResources.stream().map(e -> e.toPath().toString())
                .collect(Collectors.joining(File.pathSeparator)));

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
        } else {
            log.fine(() -> "Classes in " + project() + " are up to date.");
        }
        classSet.clear();
        @SuppressWarnings("unchecked")
        var result = (Stream<T>) Stream.of(classSet);
        return result;
    }

    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.ExceptionAsFlowControl" })
    private void compile(Resources<ClasspathElement> cpResources,
            Path destDir) {
        log.info(() -> "Compiling Java in project " + project().name());
        var classpath = cpResources.stream().map(e -> e.toPath().toString())
            .collect(Collectors.joining(File.pathSeparator));
        var javac = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var fileManager
            = javac.getStandardFileManager(diagnostics, null, null)) {
            var compilationUnits
                = fileManager.getJavaFileObjectsFromPaths(sourcePaths());
            List<String> allOptions = new ArrayList<>(options());
            allOptions.addAll(List.of(
                "-d", destDir.toString(),
                "-cp", classpath,
                "-encoding", project().get(Encoding).toString()));
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
            logDiagnostics(diagnostics);
        }
    }
}
