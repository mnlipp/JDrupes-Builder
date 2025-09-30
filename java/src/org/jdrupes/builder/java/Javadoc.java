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
import java.util.Arrays;
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
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.StreamCollector;
import static org.jdrupes.builder.java.JavaTypes.*;

/// The [Javadoc] generator provides the resource [JavadocDirectory],
/// a directory that contains the generated javadoc files.
///
/// No attempt has been made to define types for the options of
/// the javadoc tool. Rather, the options are passed as strings
/// as the [ToolProvider] API suggests. There are some noteworthy
/// exceptions for options that are directly related to resource
/// types (files, directory trees, paths) from the builder context.
///
public class Javadoc extends JavaTool {

    private final StreamCollector<FileTree<JavaSourceFile>> sources
        = StreamCollector.cached();
    private Path destination = Path.of("doc");
    private final Resources<ClasspathElement> tagletpath;
    private final List<String> taglets = new ArrayList<>();

    /// Instantiates a new java compiler.
    ///
    /// @param project the project
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public Javadoc(Project project) {
        super(project);
        tagletpath = project().newResource(new ResourceType<>() {});
    }

    /// Returns the destination directory. Defaults to "`doc`".
    ///
    /// @return the destination
    ///
    public Path destination() {
        return destination;
    }

    /// Sets the destination directory. The [Path] is resolved against
    /// the project's build directory (see [Project#buildDirectory]).
    ///
    /// @param destination the new destination
    /// @return the java compiler
    ///
    public Javadoc destination(Path destination) {
        this.destination = destination;
        return this;
    }

    /// Adds the source tree.
    ///
    /// @param sources the sources
    /// @return the java compiler
    ///
    @SafeVarargs
    public final Javadoc addSources(FileTree<JavaSourceFile>... sources) {
        this.sources.add(Arrays.stream(sources));
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
    public final Javadoc addSources(Path directory, String pattern) {
        addSources(
            project().newResource(JavaSourceTreeType, directory, pattern));
        return this;
    }

    /// Adds the sources.
    ///
    /// @param sources the sources
    /// @return the java compiler
    ///
    public final Javadoc addSources(Stream<FileTree<JavaSourceFile>> sources) {
        this.sources.add(sources);
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

    /// Adds the given elements to the taglepath.
    ///
    /// @param classpathElements the classpath elements
    /// @return the javadoc
    ///
    public Javadoc tagletpath(Stream<ClasspathElement> classpathElements) {
        tagletpath.addAll(classpathElements);
        return this;
    }

    /// Adds the given taglets.
    ///
    /// @param taglets the taglets
    /// @return the javadoc
    ///
    public Javadoc taglets(Stream<String> taglets) {
        this.taglets.addAll(taglets.toList());
        return this;
    }

    @Override
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.ExceptionAsFlowControl" })
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.includes(JavadocDirectoryType)
            && !requested.includes(CleanlinessType)) {
            return Stream.empty();
        }

        // Get destination and check if we only have to cleanup.
        var destDir = project().buildDirectory().resolve(destination);
        var generated = project().newResource(ClassTreeType, destDir, "**/*");
        if (requested.includes(CleanlinessType)) {
            generated.delete();
            destDir.toFile().delete();
            return Stream.empty();
        }

        // Generate
        var javadoc = ToolProvider.getSystemDocumentationTool();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var fileManager
            = javadoc.getStandardFileManager(diagnostics, null, null)) {
            if (options().contains("-d")) {
                new BuildException(project()
                    + ": Specifying the destination directory with "
                    + "options() is not allowed.");
            }
            List<String> allOptions = new ArrayList<>(options());
            allOptions.addAll(List.of("-d", destDir.toString()));
            var tagletPath = tagletPath();
            if (!tagletPath.isEmpty()) {
                allOptions.addAll(List.of("-tagletpath", tagletPath));
            }
            for (var taglet : taglets) {
                allOptions.addAll(List.of("-taglet", taglet));
            }
            var sourceFiles
                = fileManager.getJavaFileObjectsFromPaths(sourcePaths());
            if (!javadoc.getTask(null, fileManager, diagnostics, null,
                allOptions, sourceFiles).call()) {
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
        @SuppressWarnings("unchecked")
        var result = (Stream<T>) Stream
            .of(project().newResource(JavadocDirectoryType, destDir));
        return result;
    }

    private String tagletPath() {
        return tagletpath.stream().<Path> mapMulti((e, consumer) -> {
            if (e instanceof ClassTree classTree) {
                consumer.accept(classTree.root());
            } else if (e instanceof JarFile jarFile) {
                consumer.accept(jarFile.path());
            }
        }).map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    }
}
