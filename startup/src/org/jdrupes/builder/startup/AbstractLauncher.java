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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Masked;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.DefaultBuildContext;
import org.jdrupes.builder.core.DefaultFileTree;
import static org.jdrupes.builder.java.JavaTypes.*;

/// A default implementation of a [Launcher].
///
public abstract class AbstractLauncher implements Launcher {

    /// The JDrupes Builder properties read from the file
    /// `.jdbld.properties` in the root project.
    @SuppressWarnings("PMD.FieldNamingConventions")
    protected static final Properties jdbldProps;

    static {
        // Get builder configuration
        Properties fallbacks = new Properties();
        fallbacks.putAll(Map.of(DefaultBuildContext.JDBLD_DIRECTORY, "_jdbld"));
        jdbldProps = new Properties(fallbacks);
        var propsPath
            = Path.of("").toAbsolutePath().resolve(".jdbld.properties");
        if (propsPath.toFile().canRead()) {
            try {
                jdbldProps.load(Files.newBufferedReader(propsPath));
            } catch (IOException e) {
                throw new BuildException(
                    "Cannot read properties from " + propsPath, e);
            }
        }

        // Get logging configuration
        InputStream props;
        try {
            props = Files.newInputStream(
                Path.of(
                    jdbldProps.getProperty(DefaultBuildContext.JDBLD_DIRECTORY),
                    "logging.properties"));
        } catch (IOException e) {
            props = BootstrapLauncher.class
                .getResourceAsStream("logging.properties");
        }
        // Get logging properties from file and put them in effect
        try (var from = props) {
            LogManager.getLogManager().readConfiguration(from);
        } catch (SecurityException | IOException e) {
            e.printStackTrace(); // NOPMD
        }
    }

    /// The log.
    protected final Logger log = Logger.getLogger(getClass().getName());

    /// Instantiates a new abstract launcher.
    ///
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public AbstractLauncher() {
        // Makes javadoc happy.
    }

    /// Find projects. The classpath is scanned for classes that implement
    /// [Project] but do not implement [Masked].
    ///
    /// @param clsLoader the cls loader
    /// @param rootProjects classes that implement [RootProject]
    /// @param subprojects classes that implement [Project] but not
    /// [RootProject]
    ///
    @SuppressWarnings({ "unchecked", "PMD.AvoidLiteralsInIfCondition" })
    protected void findProjects(ClassLoader clsLoader,
            List<Class<? extends RootProject>> rootProjects,
            List<Class<? extends Project>> subprojects) {
        List<URL> classDirUrls;
        try {
            classDirUrls = Collections.list(clsLoader.getResources(""));
        } catch (IOException e) {
            throw new BuildException("Problem scanning classpath", e);
        }
        classDirUrls.parallelStream()
            .filter(uri -> !"jar".equals(uri.getProtocol())).map(uri -> {
                try {
                    return Path.of(uri.toURI());
                } catch (URISyntaxException e) {
                    throw new BuildException("Problem scanning classpath", e);
                }
            })
            .map(p -> DefaultFileTree.create(null, ClassTreeType, p,
                "**/*.class",
                false))
            .flatMap(FileTree::entries).map(Path::toString)
            .map(p -> p.substring(0, p.length() - 6).replace('/', '.'))
            .map(cn -> {
                try {
                    return clsLoader.loadClass(cn);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(
                        "Cannot load detected class", e);
                }
            }).forEach(cls -> {
                if (!Masked.class.isAssignableFrom(cls) && !cls.isInterface()
                    && !Modifier.isAbstract(cls.getModifiers())) {
                    if (RootProject.class.isAssignableFrom(cls)) {
                        rootProjects.add((Class<? extends RootProject>) cls);
                    } else if (Project.class.isAssignableFrom(cls)) {
                        subprojects.add((Class<? extends Project>) cls);
                    }
                }
            });
        if (rootProjects.isEmpty()) {
            throw new BuildException("No project implements RootProject");
        }
        if (rootProjects.size() > 1) {
            StringBuilder msg = new StringBuilder(50);
            msg.append("More than one project implements RootProject: ");
            rootProjects.stream().map(Class::getName)
                .collect(Collectors.joining(", "));
            throw new BuildException("No project implements RootProject");
        }
    }

    /// A utility method that invokes the callable. If an exception
    /// occurs during the invocation, it unwraps the causes until it
    /// finds the root [BuildException], prints the message from this
    /// exception and exits.
    ///
    /// @param <T> the generic type
    /// @param todo the todo
    /// @return the t
    ///
    @SuppressWarnings({ "PMD.AvoidReassigningCatchVariables",
        "PMD.DoNotTerminateVM", "PMD.AvoidCatchingGenericException",
        "PMD.AvoidInstanceofChecksInCatchClause" })
    protected final <T> T unwrapBuildException(Callable<T> todo) {
        try {
            return todo.call();
        } catch (Exception e) {
            Throwable checking = e;
            Throwable cause = e;
            BuildException bldEx = null;
            while (checking != null) {
                if (checking instanceof BuildException exc) {
                    bldEx = exc;
                    cause = exc.getCause();
                }
                checking = checking.getCause();
            }
            final var finalBldEx = bldEx;
            if (bldEx == null) {
                log.log(Level.SEVERE, e,
                    () -> "Starting builder failed: " + e.getMessage());
            } else if (cause == null) {
                log.severe(() -> "Build failed: " + finalBldEx.getMessage());
            } else {
                log.log(Level.SEVERE, cause,
                    () -> "Build failed: " + finalBldEx.getMessage());
            }
            System.exit(1);
            return null;
        }
    }
}
