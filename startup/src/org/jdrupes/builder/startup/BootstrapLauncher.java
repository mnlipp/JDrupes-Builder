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

import java.util.Collections;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.LauncherSupport;

/// A default implementation of a [Launcher]. The launcher first builds
/// the user's JDrupes Builder project, using the JDrupes Builder project
/// defined by [BootstrapRoot] and [BootstrapBuild]. The default action
/// of [BootstrapRoot] adds the results from the bootstrap build 
/// to the classpath and launches the actual JDrupes Builder project.
///
public class BootstrapLauncher extends AbstractLauncher {

    /// The log.
    protected final Logger log = Logger.getLogger(getClass().getName());
    /* default */static String[] forwardedArgs;
    private RootProject rootProject;

    /// Instantiates a new bootstrap launcher. An instance of the class
    /// passed as argument is created and used as root project for the
    /// build.
    ///
    /// Unless the root project is the only project, the root project
    /// must declare dependencies, else the subprojects won't be
    /// instantiated.
    ///
    /// @param rootProject the root project
    ///
    public BootstrapLauncher(Class<? extends RootProject> rootProject) {
        unwrapBuildException(() -> {
            this.rootProject = LauncherSupport.createProjects(rootProject,
                Collections.emptyList(), jdbldProps);
            this.rootProject.execute("bootstrap");
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

    /// The main method.
    ///
    /// @param args the arguments
    ///
    public static void main(String[] args) {
        forwardedArgs = args;
        new BootstrapLauncher(BootstrapRoot.class);
    }
}
