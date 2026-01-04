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

package org.jdrupes.builder.startup;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
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
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Masked;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.LauncherSupport;
import org.jdrupes.builder.java.JarFile;
import static org.jdrupes.builder.java.JavaTypes.*;

/// An implementation of a [Launcher] that expects that the JDrupes
/// Builder project already been compiled and its classes are available
/// on the classpath.
///
public class DirectLauncher extends AbstractLauncher {

    private static final String RUNTIME_EXTENSIONS = "runtimeExtensions";
    private RootProject rootProject;

    /// Instantiates a new direct launcher. The classpath is scanned for
    /// classes that implement [Project] but do not implement [Masked].
    /// One of these must also implement the [RootProject] interface.
    /// The latter is instantiated and registered as root project with all
    /// other classes found as direct sub projects.
    ///
    /// @param classloader the classloader
    /// @param args the arguments
    ///
    @SuppressWarnings({ "PMD.UseVarargs" })
    public DirectLauncher(ClassLoader classloader, String[] args) {
        super(args);
        unwrapBuildException(() -> {
            final var extClsLdr = addRuntimeExts(classloader);
            var rootProjects = new ArrayList<Class<? extends RootProject>>();
            var subprojects = new ArrayList<Class<? extends Project>>();
            findProjects(extClsLdr, rootProjects, subprojects);
            rootProject = LauncherSupport.createProjects(rootProjects.get(0),
                subprojects, jdbldProps, commandLine);
            return null;
        });
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
                log.log(Level.WARNING, e, () -> "Cannot convert " + jf
                    + " to URL: " + e.getMessage());
            }
        }).toArray(URL[]::new);

        // Return augmented classloader
        return new URLClassLoader(cpUrls, classloader);
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
                throw new BuildException(
                    "Cannot resolve: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public <T extends Resource> Stream<T> provide(ResourceRequest<T> request) {
        return unwrapBuildException(() -> {
            // Provide requested resource, handling all exceptions here
            var result = rootProject.get(request).toList();
            return result.stream();
        });
    }

    /// Execute a command.
    ///
    /// @param args the args
    ///
    @SuppressWarnings("PMD.SystemPrintln")
    public void command(String... args) {
        CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse(baseOptions(), args);
        } catch (ParseException e) {
            throw new BuildException(e);
        }
        for (var arg : commandLine.getArgs()) {
            var reqs = LauncherSupport.lookupCommand(rootProject, arg);
            if (reqs.length == 0) {
                throw new BuildException("Unknown command: " + arg);
            }
            for (var req : reqs) {
                provide(req).forEach(r -> System.out.println(r.toString()));
            }
        }
    }

    /// This main can be used to start the user's JDrupes Builder
    /// project from an IDE for debugging purposes. It expects that
    /// the JDrupes Builder project has already been compiled (typically
    /// by the IDE) and is available on the classpath.
    ///
    /// @param args the arguments
    ///
    public static void main(String[] args) {
        new DirectLauncher(Thread.currentThread().getContextClassLoader(),
            args).command(args);
    }
}
