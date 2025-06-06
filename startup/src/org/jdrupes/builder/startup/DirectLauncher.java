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

import java.util.ArrayList;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Masked;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.LauncherSupport;

/// An implementation of a [Launcher] that expects that the JDrupes
/// Builder project already been compiled and its classes are available
/// on the classpath.
///
public class DirectLauncher extends AbstractLauncher {

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
    @SuppressWarnings("PMD.UseVarargs")
    public DirectLauncher(ClassLoader classloader, String[] args) {
        unwrapBuildException(() -> {
            var rootProjects = new ArrayList<Class<? extends RootProject>>();
            var subprojects = new ArrayList<Class<? extends Project>>();
            findProjects(classloader, rootProjects, subprojects);
            rootProject = LauncherSupport
                .createProjects(rootProjects.get(0), subprojects, jdbldProps);
            for (var arg : args) {
                rootProject.execute(arg);
            }
            return null;
        });
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
        new DirectLauncher(Thread.currentThread().getContextClassLoader(),
            args);
    }
}
