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

import java.nio.file.Path;
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
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.Resources;
import static org.jdrupes.builder.java.JavaConsts.*;

public class JavaDoc extends JavaTool<FileTree<FileResource>> {

    private final Resources<FileTree<JavaSourceFile>> sources
        = project().newResources(FileResource.class);

    /// Instantiates a new java compiler.
    ///
    /// @param project the project
    ///
    public JavaDoc(Project project) {
        super(project);
    }

    /// Adds the source tree.
    ///
    /// @param sources the sources
    /// @return the java compiler
    ///
    public final JavaDoc addSources(FileTree<JavaSourceFile> sources) {
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
    public final JavaDoc addSources(Path directory, String pattern) {
        addSources(
            project().newFileTree(directory, pattern, JavaSourceFile.class));
        return this;
    }

    /// Adds the sources.
    ///
    /// @param sources the sources
    /// @return the java compiler
    ///
    public final JavaDoc
            addSources(Stream<FileTree<JavaSourceFile>> sources) {
        this.sources.addAll(sources);
        return this;
    }

    /// Source paths.
    ///
    /// @return the collection
    ///
    private Collection<Path> sourcePaths() {
        return sources.stream().map(Resources::stream)
            .flatMap(Function.identity()).map(FileResource::path)
            .collect(Collectors.toSet());
    }

    @Override
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.ExceptionAsFlowControl" })
    public <T extends Resource> Stream<T>
            provide(ResourceRequest<T> requested) {
        if (!requested.type().isAssignableFrom(JAVADOC_DIRECTORY)) {
            return Stream.empty();
        }

        var javadoc = ToolProvider.getSystemDocumentationTool();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var destDir = project().buildDirectory().resolve("doc");
        try (var fileManager
            = javadoc.getStandardFileManager(diagnostics, null, null)) {
            var sourceFiles
                = fileManager.getJavaFileObjectsFromPaths(sourcePaths());
            if (!javadoc.getTask(null, fileManager, diagnostics, null,
                List.of(// "-locale", "en_US",
                    "-d", destDir.toString(), "-quiet"),
                sourceFiles)
                .call()) {
                throw new BuildException("Documentation generation failed");
            }
        } catch (Exception e) {
            log.log(java.util.logging.Level.SEVERE, () -> "Project "
                + project().name() + ": " + "Problem compiling Java: "
                + e.getMessage());
            throw new BuildException(e);
        } finally {
            logDiagnostics(diagnostics);
        }
        return Stream.empty();
    }

}
