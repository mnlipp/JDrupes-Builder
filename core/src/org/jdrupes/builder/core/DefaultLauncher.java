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
import java.util.concurrent.Callable;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ClassFile;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
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
        unwrapBuildException(() -> {
            initRootProject(rootProject);
            ((AbstractProject) this.rootProject).createProjects();
            return null;
        });
    }

    /// Instantiates a new default launcher. The classpath is scanned.
    /// The project that implements the [RootProject] interface is
    /// registered as root project and all classes implementing the
    /// [Project] interface are registered as sub projects.
    ///
    /// @param clsLoader the class loader
    ///
    public DefaultLauncher(ClassLoader clsLoader) {
        unwrapBuildException(() -> {
            initWithReflection(clsLoader);
            ((AbstractProject) this.rootProject).createProjects();
            return null;
        });
    }

    @SuppressWarnings({ "unchecked", "PMD.AvoidLiteralsInIfCondition" })
    private void initWithReflection(ClassLoader clsLoader) {
        List<URL> classDirUrls;
        try {
            classDirUrls = Collections.list(clsLoader.getResources(""));
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
        }).map(p -> new DefaultFileTree<ClassFile>(null, p, "**/*.class",
            ClassFile.class, false))
            .flatMap(FileTree::entries).map(Path::toString)
            .map(p -> p.substring(0, p.length() - 6).replace('/', '.'))
            .filter(n -> !n.startsWith("org.jdrupes.builder.startup"))
            .map(cn -> {
                try {
                    return clsLoader.loadClass(cn);
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
        "PMD.DoNotTerminateVM", "PMD.AvoidCatchingGenericException" })
    private <T> T unwrapBuildException(Callable<T> todo) {
        try {
            return todo.call();
        } catch (Exception e) {
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
            return null;
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

    @Override
    public <T extends Resource> Stream<T>
            provide(ResourceRequest<T> requested) {
        return unwrapBuildException(() -> {
            // Provide requested resource, handling all exceptions here
            var result = rootProject.provide(requested).toList();
            return result.stream();
        });
    }

    /// Start.
    ///
    /// @param args the args
    ///
    @Override
    @SuppressWarnings({ "PMD.AvoidPrintStackTrace", "PMD.SystemPrintln" })
    public void start(String[] args) {
        provide(new ResourceRequest<>(new ResourceType<FileResource>() {
        })).forEach(r -> {
            System.out.println(r);
        });
    }

    /// The main method.
    ///
    /// @param args the arguments
    ///
    public static void main(String[] args) {
        new DefaultLauncher(Thread.currentThread().getContextClassLoader())
            .start(args);
    }
}
