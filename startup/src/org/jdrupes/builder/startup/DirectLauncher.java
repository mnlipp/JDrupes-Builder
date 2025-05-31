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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractLauncher;

/// An implementation of a [Launcher] that expects that the JDrupes
/// Builder project already been compiled and its classes are available
/// on the classpath.
///
public class DirectLauncher extends AbstractLauncher {

    protected final Logger log = Logger.getLogger(getClass().getName());

    private RootProject rootProject;

    /// Instantiates a new direct launcher. The classpath is scanned
    /// for classes that implement [Project]. One of these must also
    /// implement the [RootProject] interface. This class is instantiated
    /// and registered as root project. All other classes implementing
    /// the [Project] interface are registered as sub projects.
    ///
    /// @param classLoader the class loader
    ///
    public DirectLauncher(ClassLoader classloader) {
        unwrapBuildException(() -> {
            var rootProjects = new ArrayList<Class<? extends RootProject>>();
            var subprojects = new ArrayList<Class<? extends Project>>();
            findProjects(classloader, rootProjects, subprojects);
            rootProject = createProjects(rootProjects.get(0), subprojects);
            rootProject.provide();
            return null;
        });
    }

    static {
        InputStream props;
        try {
            props = Files.newInputStream(
                Path.of("_jdbld", "logging.properties"));
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

    @Override
    public <T extends Resource> Stream<T>
            provide(ResourceRequest<T> requested) {
        return unwrapBuildException(() -> {
            // Provide requested resource, handling all exceptions here
            var result = rootProject.provide(requested).toList();
            return result.stream();
        });
    }

    /// This main can be used to start the user's JDrupes Builder
    /// project from an IDE for debugging purposes. It expects that
    /// the JDrupes Builder project has already been compiled (typically
    /// by the IDE) and is available on the classpath.
    ///
    /// @param args the arguments
    ///
    public static void main(String[] args) {
        new DirectLauncher(Thread.currentThread().getContextClassLoader());
    }
}
