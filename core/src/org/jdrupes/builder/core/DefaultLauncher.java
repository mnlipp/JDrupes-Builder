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

package org.jdrupes.builder.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.jdrupes.builder.api.AllResources;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.RootProject;

/// A default implementation of a [Launcher].
///
public class DefaultLauncher implements Launcher {

    protected final Logger log = Logger.getLogger(getClass().getName());

    private Project rootProject;

    /// Instantiates a new default launcher. In instance of the class
    /// passed as argument is created and registered as root project.
    ///
    /// Unless the root project is the only project, the root project
    /// must use [AbstractProject#AbstractProject(Class...)] as its 
    /// super constructor to register sub projects.
    ///
    /// @param rootProject the root project
    ///
    public DefaultLauncher(Class<? extends Project> rootProject) {
        initRootProject(rootProject);
    }

    /// Instantiates a new default launcher. The classpath is scanned.
    /// The project that implements the [RootProject] interface is
    /// registered as root project and all classes implementing the
    /// [Project] interface are registered as sub projects.
    ///
    public DefaultLauncher() {
        unwrapBuildException(this::initWithReflection);
    }

    @SuppressWarnings({ "unchecked", "PMD.AvoidLiteralsInIfCondition" })
    private void initWithReflection() {
        List<URL> classDirUrls;
        try {
            classDirUrls = Collections.list(Thread.currentThread()
                .getContextClassLoader().getResources(""));
        } catch (IOException e) {
            throw new BuildException("Problem scanning classpath", e);
        }
        var rootClasses = new ArrayList<Class<? extends RootProject>>();
        var subClasses = new ArrayList<Class<? extends Project>>();
        classDirUrls.parallelStream().map(uri -> {
            try {
                return Path.of(uri.toURI());
            } catch (URISyntaxException e) {
                throw new BuildException("Problem scanning classpath", e);
            }
        }).map(p -> new DefaultFileTree(null, p, "**/*.class",
            Resource.KIND_CLASSES))
            .flatMap(FileTree::entries).map(Path::toString)
            .map(p -> p.substring(0, p.length() - 6).replace('/', '.'))
            .map(cn -> {
                try {
                    return Thread.currentThread()
                        .getContextClassLoader().loadClass(cn);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(
                        "Cannot load detected class", e);
                }
            }).forEach(cls -> {
                if (!cls.isInterface()
                    && !Modifier.isAbstract(cls.getModifiers())) {
                    if (RootProject.class.isAssignableFrom(cls)) {
                        rootClasses.add((Class<? extends RootProject>) cls);
                    } else if (Project.class.isAssignableFrom(cls)) {
                        subClasses.add((Class<? extends Project>) cls);
                    }
                }
            });
        if (rootClasses.size() != 1) {
            throw new BuildException(
                "Exactly one project must implement RootProject");
        }
        AbstractProject.detectedSubprojects(subClasses);
        initRootProject(rootClasses.get(0));
    }

    private void initRootProject(Class<? extends Project> rootProject) {
        try {
            this.rootProject = rootProject.getConstructor().newInstance();
        } catch (NoSuchMethodException | SecurityException
                | NegativeArraySizeException | InstantiationException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings({ "PMD.AvoidReassigningCatchVariables",
        "PMD.DoNotTerminateVM" })
    private void unwrapBuildException(Runnable todo) {
        try {
            todo.run();
        } catch (BuildException e) {
            var cause = e.getCause();
            while (cause != null) {
                if (cause instanceof BuildException nbe) {
                    e = nbe;
                }
                cause = cause.getCause();
            }
            final var rootCase = e;
            log.severe(() -> "Build failed: " + rootCase.getMessage());
            System.exit(1);
        }

    }

    static {
        InputStream props;
        try {
            props = Files.newInputStream(
                Path.of("_jdbld", "logging.properties"));
        } catch (IOException e) {
            props = DefaultLauncher.class
                .getResourceAsStream("logging.properties");
        }
        // Get logging properties from file and put them in effect
        try (var from = props) {
            LogManager.getLogManager().readConfiguration(from);
        } catch (SecurityException | IOException e) {
            e.printStackTrace(); // NOPMD
        }
    }

    /// Start.
    ///
    /// @param args the args
    ///
    @Override
    @SuppressWarnings({ "PMD.AvoidPrintStackTrace", "PMD.SystemPrintln" })
    public void start(String[] args) {
        // Start building
        unwrapBuildException(() -> {
            // Finish project creation
            ((AbstractProject) rootProject).createProjects();

            // Provide requested resource
            rootProject.provide(AllResources.of(Resource.KIND_APP_JAR))
                .forEach(r -> {
                    System.out.println(r);
                });
        });
    }

    public static void main(String[] args) {
        new DefaultLauncher().start(args);
    }
}
