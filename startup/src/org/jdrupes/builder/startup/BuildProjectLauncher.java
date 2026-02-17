/*
 * JDrupes Builder
 * Copyright (C) 2025, 2026 Michael N. Lipp
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

package org.jdrupes.builder.startup;

import com.google.common.flogger.FluentLogger;
import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ConfigurationException;
import org.jdrupes.builder.api.FaultAware;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Masked;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.api.UnavailableException;
import org.jdrupes.builder.core.AbstractRootProject;
import org.jdrupes.builder.core.DefaultBuildContext;
import org.jdrupes.builder.java.JarFile;
import static org.jdrupes.builder.java.JavaTypes.*;

/// An implementation of a [Launcher] that launches the build configuration.
/// It expects that the JDrupes Builder project has already been compiled
/// and its classes are available on the classpath.
///
public class BuildProjectLauncher extends AbstractLauncher
        implements AutoCloseable {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    /// The JDrupes Builder properties read from the file
    /// `.jdbld.properties` in the root project.
    protected Properties jdbldProps;
    /// The command line.
    protected CommandLine commandLine;
    private final Path buildRoot;
    private static final String RUNTIME_EXTENSIONS = "runtimeExtensions";
    private final ClassLoader extClsLdr;
    private AbstractRootProject rootProject;

    /// Instantiates a new build project launcher. The classpath is scanned
    /// for classes that implement [Project] but do not implement [Masked].
    /// One of these must also implement the [RootProject] interface.
    /// The latter is instantiated and registered as root project with all
    /// other classes found as direct sub projects.
    ///
    /// @param classloader the classloader for the build project
    /// @param buildRoot the build root
    /// @param args the arguments. Flags are processed in the constructor,
    /// command line arguments are processed in [runCommands].
    ///
    @SuppressWarnings({ "PMD.UseVarargs",
        "PMD.ConstructorCallsOverridableMethod" })
    public BuildProjectLauncher(ClassLoader classloader, Path buildRoot,
            String[] args) {
        this.buildRoot = buildRoot;
        jdbldProps = propertiesFromFiles(buildRoot);
        try {
            commandLine = new DefaultParser().parse(baseOptions(), args);
        } catch (ParseException e) {
            configureLogging(buildRoot, jdbldProps);
            throw new ConfigurationException().cause(e);
        }
        addCliProperties(jdbldProps, commandLine);
        configureLogging(buildRoot, jdbldProps);
        extClsLdr = addRuntimeExts(classloader);
        regenerateRootProject();
    }

    @Override
    public AbstractRootProject regenerateRootProject() {
        var rootProjects = new ArrayList<Class<? extends RootProject>>();
        var subprojects = new ArrayList<Class<? extends Project>>();
        findProjects(extClsLdr, rootProjects, subprojects);
        @SuppressWarnings("PMD.CloseResource")
        var newRootProject = DefaultBuildContext.createProjects(buildRoot,
            rootProjects.get(0), subprojects, jdbldProps, commandLine);
        if (rootProject != null) {
            rootProject.close();
        }
        rootProject = newRootProject;
        return rootProject;
    }

    private ClassLoader addRuntimeExts(ClassLoader classloader) {
        String[] coordinates = Arrays
            .asList(jdbldProps.getProperty(RUNTIME_EXTENSIONS, "").split(","))
            .stream()
            .map(String::trim).filter(c -> !c.isBlank()).toArray(String[]::new);
        if (coordinates.length == 0) {
            return classloader;
        }

        // Resolve using maven repo
        var cpUrls = resolveRequested(coordinates).mapMulti((jf, consumer) -> {
            try {
                consumer.accept(jf.path().toFile().toURI().toURL());
            } catch (MalformedURLException e) {
                logger.atWarning().withCause(e).log("Cannot convert %s to URL",
                    jf);
            }
        }).toArray(URL[]::new);

        // Return augmented classloader
        return new URLClassLoader(cpUrls, classloader);
    }

    @Override
    public void close() {
        rootProject.close();
    }

    @SuppressWarnings({ "PMD.UseVarargs",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    private Stream<JarFile> resolveRequested(String[] coordinates) {
        ContextOverrides overrides = ContextOverrides.create()
            .withUserSettings(true).build();
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(overrides)) {
            CollectRequest collectRequest = new CollectRequest()
                .setRepositories(context.remoteRepositories());
            for (var coord : coordinates) {
                collectRequest.addDependency(
                    new Dependency(new DefaultArtifact(coord), "runtime"));
            }

            DependencyRequest dependencyRequest
                = new DependencyRequest(collectRequest, null);
            DependencyNode rootNode;
            try {
                rootNode = context.repositorySystem()
                    .resolveDependencies(context.repositorySystemSession(),
                        dependencyRequest)
                    .getRoot();
                PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
                rootNode.accept(nlg);
                List<DependencyNode> dependencyNodes = nlg.getNodes();
                return dependencyNodes.stream()
                    .filter(d -> d.getArtifact() != null)
                    .map(d -> d.getArtifact().getFile().toPath())
                    .map(p -> ResourceFactory
                        .create(JarFileType, p));
            } catch (DependencyResolutionException e) {
                throw new ConfigurationException()
                    .message("Cannot resolve: %s", e).cause(e);
            }
        }
    }

    @Override
    public AbstractRootProject rootProject() {
        return rootProject;
    }

    /// Execute the commands from the command line.
    ///
    /// @return true, if successful
    ///
    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    public boolean runCommands() {
        for (var arg : commandLine.getArgs()) {
            var parts = arg.split(":");
            String resource = parts[parts.length - 1];
            var cmdData = rootProject.lookupCommand(resource);
            if (cmdData.requests().length == 0) {
                rootProject.context().out()
                    .println("Unknown command: " + resource);
                throw new UnavailableException().from(rootProject);
            }
            String pattern = cmdData.pattern();
            if (parts.length > 1) {
                pattern = parts[0];
            }
            for (var req : cmdData.requests()) {
                if (!resources(rootProject.projects(pattern), req)
                    // eliminate duplicates
                    .collect(Collectors.toSet()).stream()
                    // output generated resources
                    .peek(r -> rootProject.context().out()
                        .println(r.toString()))
                    .map(r -> !(r instanceof FaultAware far)
                        || !far.isFaulty())
                    .reduce(true, (r1, r2) -> r1 && r2)) {
                    return false;
                }
            }
        }
        return true;
    }

    /// This main can be used to start the user's JDrupes Builder
    /// project from an IDE for debugging purposes. It expects that
    /// the JDrupes Builder project has already been compiled (typically
    /// by the IDE) and is available on the classpath.
    ///
    /// @param args the arguments
    ///
    @SuppressWarnings("PMD.SystemPrintln")
    public static void main(String[] args) {
        try {
            if (!reportBuildException(() -> {
                try (var bpl = new BuildProjectLauncher(
                    Thread.currentThread().getContextClassLoader(),
                    Path.of("").toAbsolutePath(), args)) {
                    return bpl.runCommands();
                }
            })) {
                java.lang.Runtime.getRuntime().exit(1);
            }
        } catch (BuildException e) {
            if (e.getCause() == null) {
                logger.atSevere().log("Build failed: %s",
                    formatter().summary(e));
            } else {
                logger.atSevere().withCause(e).log("Build failed: %s",
                    formatter().summary(e));
            }
            System.out.println(formatter().summary(e));
            System.out.println(e.details());
            java.lang.Runtime.getRuntime().exit(2);
        }
    }
}
