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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.LogManager;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ConfigurationException;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Masked;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.BuildExceptionFormatter;
import org.jdrupes.builder.core.DefaultBuildContext;
import org.jdrupes.builder.core.DefaultBuildExceptionFormatter;
import org.jdrupes.builder.java.ClassTree;
import static org.jdrupes.builder.java.JavaTypes.*;

/// A default implementation of a [Launcher].
///
@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
public abstract class AbstractLauncher implements Launcher {

    /// The log.
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final BuildExceptionFormatter defaultFormatter
        = new DefaultBuildExceptionFormatter();

    /// Initializes a new abstract launcher.
    ///
    protected AbstractLauncher() {
        // Makes javadoc happy
    }

    /// Get the properties from the properties files in the user's home 
    /// directory and the build root directory.
    ///
    /// @param buildRoot the build root directory
    /// @return the properties
    ///
    protected static Properties propertiesFromFiles(Path buildRoot) {
        Properties fallbacks = new Properties();
        fallbacks.putAll(Map.of(DefaultBuildContext.JDBLD_DIRECTORY, "_jdbld"));
        for (Path propsPath : List.of(
            Path.of(System.getProperty("user.home"))
                .resolve(".jdbld").resolve("jdbld.properties"),
            buildRoot.resolve(".jdbld.properties"))) {
            try {
                if (propsPath.toFile().canRead()) {
                    fallbacks = new Properties(fallbacks);
                    fallbacks.load(Files.newBufferedReader(propsPath));
                }
            } catch (IOException e) {
                throw new BuildException().cause(e);
            }
        }
        return new Properties(fallbacks);
    }

    /// Adds properties or overrides existing properties with those from
    /// the command line.
    ///
    /// @param jdbldProps the jdbld props
    /// @param commandLine the command line
    ///
    protected static void addCliProperties(Properties jdbldProps,
            CommandLine commandLine) {
        jdbldProps.putAll(commandLine.getOptionProperties("P"));
    }

    /// Configure the logging from logging properties found in
    /// `DefaultBuildContext.JDBLD_DIRECTORY` resolved against `buildRoot`.
    ///
    /// @param buildRoot the build root
    /// @param jdbldProps the jdbld properties
    ///
    protected static void configureLogging(Path buildRoot,
            Properties jdbldProps) {
        // Get logging configuration
        InputStream props;
        try {
            props = Files.newInputStream(Path.of(
                jdbldProps.getProperty(DefaultBuildContext.JDBLD_DIRECTORY),
                "logging.properties"));
        } catch (IOException e) {
            props = BootstrapProjectLauncher.class
                .getResourceAsStream("logging.properties");
        }
        // Get logging properties from file and put them in effect
        try (var from = props) {
            LogManager.getLogManager().readConfiguration(from);
        } catch (SecurityException | IOException e) {
            e.printStackTrace(); // NOPMD
        }
    }

    /// Return the handled options.
    ///
    /// @return the options
    ///
    protected final Options baseOptions() {
        Options options = new Options();
        options.addOption("B-x", true, "Exclude from project scan");
        options.addOption(Option.builder("P").hasArgs().valueSeparator('=')
            .desc("Property in form key=value").get());
        return options;
    }

    /// Find projects. The classpath is scanned for classes that implement
    /// [Project] but do not implement [Masked].
    ///
    /// @param clsLoader the cls loader
    /// @param rootProjects classes that implement [RootProject]
    /// @param subprojects classes that implement [Project] but not
    /// [RootProject]
    ///
    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition" })
    protected void findProjects(ClassLoader clsLoader,
            List<Class<? extends RootProject>> rootProjects,
            List<Class<? extends Project>> subprojects) {
        List<URL> classDirUrls;
        try {
            classDirUrls = Collections.list(clsLoader.getResources(""));
        } catch (IOException e) {
            throw new BuildException().cause(e);
        }
        Map<Path, List<Class<? extends RootProject>>> rootProjectMap
            = new ConcurrentHashMap<>();
        classDirUrls.parallelStream()
            .filter(uri -> !"jar".equals(uri.getProtocol())).map(uri -> {
                try {
                    return Path.of(uri.toURI());
                } catch (URISyntaxException e) {
                    throw new BuildException().cause(e);
                }
            }).map(p -> ResourceFactory.create(ClassTreeType, p, "**/*.class",
                false))
            .forEach(tree -> searchTree(clsLoader, rootProjectMap, subprojects,
                tree));
        if (rootProjectMap.isEmpty()) {
            throw new ConfigurationException()
                .message("No project implements RootProject");
        }
        if (rootProjectMap.size() > 1) {
            StringBuilder msg = new StringBuilder(50);
            msg.append("More than one class implements RootProject: ")
                .append(rootProjectMap.entrySet().stream()
                    .map(e -> e.getValue().get(0).getName() + " (in "
                        + e.getKey() + ")")
                    .collect(Collectors.joining(", ")));
            throw new ConfigurationException().message(msg.toString());
        }
        rootProjects.addAll(rootProjectMap.values().iterator().next());
    }

    @SuppressWarnings("unchecked")
    private void searchTree(ClassLoader clsLoader,
            Map<Path, List<Class<? extends RootProject>>> rootProjects,
            List<Class<? extends Project>> subprojects, ClassTree tree) {
        tree.entries().map(Path::toString)
            .map(p -> p.substring(0, p.length() - 6).replace('/', '.'))
            .map(cn -> {
                try {
                    return clsLoader.loadClass(cn);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(
                        "Cannot load detected class", e);
                }
            }).forEach(cls -> {
                if (!Masked.class.isAssignableFrom(cls)
                    && !cls.isInterface()
                    && !Modifier.isAbstract(cls.getModifiers())) {
                    if (RootProject.class.isAssignableFrom(cls)) {
                        logger.atFine().log("Found root project: %s in %s",
                            cls, tree.root());
                        rootProjects.computeIfAbsent(tree.root(),
                            _ -> new ArrayList<>())
                            .add((Class<? extends RootProject>) cls);
                    } else if (Project.class.isAssignableFrom(cls)) {
                        logger.atFiner().log("Found sub project: %s in %s",
                            cls, tree.root());
                        subprojects.add((Class<? extends Project>) cls);
                    }
                }
            });
    }

    @Override
    public <T extends Resource> Stream<T> resources(Stream<Project> projects,
            ResourceRequest<T> request) {
        var result = reportBuildException(
            () -> ((DefaultBuildContext) rootProject().context()).call(
                () -> projects.map(p -> p.resources(request)).flatMap(r -> r)
                    .toList().stream()));
        if (request.isFor(CleanlinessType)) {
            regenerateRootProject();
        }
        return result;
    }

    /// A utility method for reliably reporting problems as [BuildException]s.
    /// It invokes the callable. If a Throwable occurs, it unwraps the causes
    /// until it finds the root [BuildException] and rethrows it. Any other
    /// [Throwable] is wrapped in a new [BuildException] which is then thrown.
    /// 
    /// Effectively, the method thus either returns the requested result
    /// or a [BuildException]. 
    ///
    /// @param <T> the generic type
    /// @param todo the todo
    /// @return the result
    ///
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.PreserveStackTrace" })
    public static final <T> T reportBuildException(Callable<T> todo) {
        try {
            return todo.call();
        } catch (Throwable thrown) {
            Throwable checking = thrown;
            BuildException foundBldEx = null;
            while (checking != null) {
                if (checking instanceof BuildException exc) {
                    foundBldEx = exc;
                }
                checking = checking.getCause();
            }
            if (foundBldEx != null) {
                throw foundBldEx;
            }
            throw new BuildException().cause(thrown);
        }
    }

    /// Return the default formatter for build exceptions.
    ///
    /// @return the builds the exception formatter
    ///
    public static BuildExceptionFormatter formatter() {
        return defaultFormatter;
    }

}
