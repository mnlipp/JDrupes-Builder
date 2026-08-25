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

import com.google.common.flogger.FluentLogger;
import static com.google.common.flogger.LazyArgs.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.jdrupes.builder.api.BuildException;
import static org.jdrupes.builder.api.CoreProperties.*;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.api.UnavailableException;
import static org.jdrupes.builder.java.JavaTypes.*;

/// The [JavaCompiler] generator provides two types of resources.
///
/// 1. The [JavaSourceFile]s of the project as configured with
///    [addSources(FileTree<JavaSourceFile>)][addSources]
///    in response to a [ResourceRequest] with [ResourceType]
///    [JavaTypes#JavaSourceTreeType] (or a more general type).
///
/// 2. The [ClassFile]s that result from compiling the sources in response
///    to a [ResourceRequest] with [ResourceType]
///    [JavaTypes#ClassTreeType] (or a more general type such as
///    [JavaTypes#CodeContributionType]).
///
/// No attempt has been made to define types for the options of
/// the java compiler. Rather, the options are passed as strings
/// as the [ToolProvider] API suggests. There are some noteworthy
/// exceptions for options that are directly related to resource
/// types (files, directory trees, paths) from the builder context.
///
/// If no "`-g...`" option is specified, the generator adds "`-g`" and
/// thus generates full debug information. If you want to restore the
/// default behavior of the java compiler, you have to specify
/// "`-g:[lines, source]`" explicitly.
///
/// ## JPMS support
///
/// When the sources contain a `module-info.java`, or when the module mode
/// is set to `ModuleMode.MODULE`, compilation uses the module-path
/// (`--module-path`) for code contributions that contain a module descriptor.
/// Non-modular code contributions remain on the classpath (`-cp`). In
/// `ModuleMode.AUTO` mode (the default), module compilation is activated
/// automatically when a `module-info.java` is found among the sources.
///
public class JavaCompiler extends JavaTool {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private final Resources<FileTree<JavaSourceFile>> sources
        = Resources.of(new ResourceType<>() {});
    private Path destination;

    /// Initializes a new java compiler.
    ///
    /// @param project the project
    ///
    public JavaCompiler(Project project) {
        super(project);
        if (project instanceof MergedTestProject) {
            destination = Path.of("test-classes");
        } else {
            destination = Path.of("classes");
        }
    }

    /// Returns the destination directory. Defaults to "`classes`" for
    /// "normal" projects and to "`test-classes`" for projects that
    /// implement the [MergedTestProject] interface.
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
    /// @return the java compiler
    ///
    public final JavaCompiler addSources(Path directory, String pattern) {
        addSources(FileTree.of(
            project(), directory, JavaSourceFile.class, pattern));
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

    /// Returns the source trees configured for the compiler.
    ///
    /// @return the resources
    ///
    public Resources<FileTree<JavaSourceFile>> sources() {
        return sources;
    }

    /// Returns the source paths.
    ///
    /// @return the collection
    ///
    private Collection<Path> sourcePaths() {
        return sources.stream().map(Resources::stream)
            .flatMap(Function.identity()).map(FileResource::path)
            .collect(Collectors.toList());
    }

    @Override
    protected boolean hasModuleInfo() {
        return sources.stream().flatMap(Resources::stream)
            .anyMatch(f -> f.path().endsWith("module-info.java"));
    }

    /// Extracts the module name from `module-info.java` by parsing the
    /// first `module <name>` declaration. Returns `null` if no module
    /// descriptor source is found.
    ///
    /// @return the module name, or null
    ///
    private String extractModuleName() {
        var pattern = Pattern.compile("^\\s*module\\s+(\\S+)");
        for (var sourceTree : sources.get()) {
            for (var file : sourceTree.get()) {
                if (file.path().endsWith("module-info.java")) {
                    try {
                        var content = Files.readString(file.path());
                        Matcher matcher = pattern.matcher(content);
                        if (matcher.find()) {
                            return matcher.group(1);
                        }
                    } catch (IOException e) {
                        logger.atWarning().withCause(e)
                            .log("Cannot read %s", file.path());
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected <T extends Resource> Collection<T>
            doProvide(ResourceRequest<T> requested) {
        if (requested.accepts(JavaSourceTreeType)) {
            @SuppressWarnings({ "unchecked" })
            var result = (Collection<T>) sources.get();
            return result;
        }

        if (!requested.accepts(ClassTreeType)
            && !requested.accepts(CleanlinessType)) {
            return Collections.emptyList();
        }

        // Get this project's previously generated classes for checking
        // or deleting.
        var destDir = project().buildDirectory().resolve(destination);
        final var classSet = ClassTree.of(project(), destDir);
        if (requested.accepts(CleanlinessType)) {
            classSet.cleanup();
            return Collections.emptyList();
        }

        // Evaluate for most special type
        if (requested.accepts(ClassTreeType)
            && !requested.type().equals(ClassTreeType)) {
            @SuppressWarnings("unchecked")
            var result = (Collection<T>) resources(of(ClassTreeType)).toList();
            return result;
        }

        // Get classpath for compilation. Filter myself, in case the
        // compilation result is consumed by the project.
        var cpResources = Resources.of(CodeContributionsType).addAll(
            project().providers(Consume, Reveal, Expose).without(this)
                .resources(of(CodeContributionType)));

        // (Re-)compile only if necessary
        var useModulePath = effectiveModuleMode() == ModuleMode.MODULE;
        var expectedClassCount = sources.stream()
            .flatMap(Resources::stream).map(FileResource::path)
            .filter(p -> p.toString().endsWith(".java")
                && !p.endsWith("package-info.java")
                && (useModulePath || !p.endsWith("module-info.java")))
            .count();
        if (sources.isNewerThan(classSet)
            || cpResources.isNewerThan(classSet)
            || classSet.stream().count() < expectedClassCount) {
            classSet.cleanup();
            compile(cpResources, destDir);
        } else {
            logger.atFine().log("%s found classes to be up to date", this);
        }
        classSet.clear();
        @SuppressWarnings("unchecked")
        var result = (Collection<T>) List.of(classSet);
        return result;
    }

    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.ExceptionAsFlowControl" })
    private void compile(Resources<CodeContribution> ccResources,
            Path destDir) {
        logger.atInfo().log("Compiling Java in %s", project().name());
        var javac = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var useModulePath = effectiveModuleMode() == JavaTool.ModuleMode.MODULE;
        if (useModulePath) {
            logger.atFine().log("Using module-path compilation");
        }

        try (var fileManager
            = javac.getStandardFileManager(diagnostics, null, null)) {
            List<String> allOptions = new ArrayList<>(options());

            // If no -g... option is given, add -g (full debug info)
            if (allOptions.stream()
                .filter(o -> o.startsWith("-g")).findAny().isEmpty()) {
                allOptions.add("-g");
            }

            // Add common options
            allOptions.addAll(List.of(
                "-d", destDir.toString(),
                "-encoding", project().get(Encoding)));

            if (useModulePath) {
                compileWithModulePath(fileManager, ccResources, allOptions);
            } else {
                compileWithClasspath(fileManager, ccResources);
            }

            var compilationUnits
                = fileManager.getJavaFileObjectsFromPaths(sourcePaths());
            if (!javac.getTask(null, fileManager, diagnostics, allOptions,
                null, compilationUnits).call()) {
                throw new UnavailableException().from(this);
            }
        } catch (Exception e) {
            logger.atSevere().withCause(e)
                .log("Project %s: Problem compiling Java: %s", project().name(),
                    e.getMessage());
            throw new BuildException().from(this).cause(e);
        } finally {
            logDiagnostics(diagnostics);
            logger.atFine().log("%s finished compilation", this);
        }
    }

    /// Configures the options for plain classpath compilation.
    ///
    /// @param fileManager the file manager
    /// @param ccResources the classpath resources
    /// @param allOptions the option list to modify
    /// @throws IOException 
    ///
    private void compileWithClasspath(StandardJavaFileManager fileManager,
            Resources<CodeContribution> ccResources)
            throws IOException {
        var classpathElements = ccResources.stream().toList();
        if (!classpathElements.isEmpty()) {
            var cpPaths = classpathElements.stream()
                .map(e -> e.toPath().toFile()).toList();
            fileManager.setLocation(StandardLocation.CLASS_PATH, cpPaths);
        }
        logger.atFiner().log("%s uses classpath %s", this,
            lazy(() -> classpathElements.stream()
                .map(e -> e.toPath().toString())
                .collect(Collectors.joining(File.pathSeparator))));
    }

    /// Configures the file manager and options for module-path compilation.
    /// Modular dependencies go to --module-path, non-modular stay on -cp.
    ///
    /// @param fileManager the file manager
    /// @param ccResources the classpath resources
    /// @param allOptions the option list to modify
    /// @throws IOException 
    ///
    @SuppressWarnings("PMD.ConfusingTernary")
    private void compileWithModulePath(StandardJavaFileManager fileManager,
            Resources<CodeContribution> ccResources, List<String> allOptions)
            throws IOException {
        var moduleElements = ccResources.stream()
            .filter(cc -> cc.isModular()).toList();
        var classpathElements = ccResources.stream()
            .filter(cc -> !cc.isModular()).toList();

        if (!moduleElements.isEmpty()) {
            var modulePaths = moduleElements.stream()
                .map(e -> e.toPath().toFile()).toList();
            fileManager.setLocation(StandardLocation.MODULE_PATH, modulePaths);
            logger.atFiner().log("%s uses module-path %s", this,
                lazy(() -> moduleElements.stream()
                    .map(e -> e.toPath().toString())
                    .collect(Collectors.joining(File.pathSeparator))));
        }

        if (!classpathElements.isEmpty()) {
            var cpPaths = classpathElements.stream()
                .map(e -> e.toPath().toFile()).toList();
            fileManager.setLocation(StandardLocation.CLASS_PATH, cpPaths);

            // Allow the named module to read from the unnamed module
            var moduleName = extractModuleName();
            if (moduleName != null) {
                allOptions.add("--add-reads");
                allOptions.add(moduleName + "=ALL-UNNAMED");
            }
            logger.atFiner().log("%s uses classpath %s", this,
                lazy(() -> classpathElements.stream()
                    .map(e -> e.toPath().toString())
                    .collect(Collectors.joining(File.pathSeparator))));
        } else {
            logger.atFiner().log("%s compiling as pure module", this);
        }
    }
}
