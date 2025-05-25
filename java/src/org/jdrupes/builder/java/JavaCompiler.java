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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.jdrupes.builder.api.AllResources;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Dependency.Intend;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.OwnResources;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractGenerator;

/// The Class JavaCompiler.
///
public class JavaCompiler extends AbstractGenerator<FileTree> {

    private static Set<Intend> forCompilation
        = EnumSet.of(Intend.Consume, Intend.Expose);
    private final List<FileTree> sources = new ArrayList<>();

    /// Instantiates a new java compiler.
    ///
    /// @param project the project
    ///
    public JavaCompiler(Project project) {
        super(project);
    }

    /// Adds the sources.
    ///
    /// @param sources the sources
    /// @return the java compiler
    ///
    public JavaCompiler addSources(FileTree sources) {
        this.sources.add(sources);
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

    private String classpath(Resource resource) {
        return project()
            .provided(resource, forCompilation).<Path> mapMulti((r, sink) -> {
                if (r instanceof FileTree fileSet) {
                    sink.accept(fileSet.root());
                }
            }).map(Path::toString)
            .collect(Collectors.joining(File.pathSeparator));
    }

    /// Provide.
    ///
    /// @param resource the resource
    /// @return the stream
    ///
    @Override
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.ExceptionAsFlowControl" })
    public Stream<FileTree> provide(Resource resource) {
        if (!Resource.KIND_CLASSES.equals(resource.kind())) {
            return Stream.empty();
        }
        if (resource instanceof AllResources) {
            var own = project().build().provide(this,
                OwnResources.of(Resource.KIND_CLASSES));
            return Stream.concat(own,
                project().provided(resource, EnumSet.of(Intend.Expose)));
        }

        // Get this project's classes
        var destDir = project().buildDirectory().resolve("classes");
        final var classSet = project().newFileTree(
            project(), destDir, "**/*", Resource.KIND_CLASSES).delete();
        log.fine(() -> "Getting classpath in " + project().name());
        var classpath = classpath(resource);

        log.info(() -> "Compiling Java in " + project().name());
        var javac = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var fileManager
            = javac.getStandardFileManager(diagnostics, null, null)) {
            var compilationUnits
                = fileManager.getJavaFileObjectsFromPaths(sourcePaths());
            if (!javac.getTask(null, fileManager, null,
                List.of("-d", destDir.toString(),
                    "-cp", classpath,
                    "-Xlint:unchecked"),
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
        return Stream.of(classSet);
    }

}
