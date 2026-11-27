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

package org.jdrupes.builder.eclipse;

import java.util.List;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.java.LibraryJarFile;

/// A resource request for an Eclipse launch configuration file.
///
public interface EclipseLauncherRequest
        extends ResourceRequest<EclipseLauncher> {

    /// Returns the main class.
    ///
    /// @return the string
    ///
    String mainClass();

    /// Return the arguments to use in the launch configuration.
    ///
    /// @return the string
    ///
    String arguments();

    /// Return the VM arguments to use in the launch configuration.
    ///
    /// @return the string
    ///
    String vmArguments();

    /// Returns the projects explicitly added to the launch classpath.
    ///
    /// @return the list of projects
    ///    
    List<Project> extraProjects();

    /// Returns the library JAR files explicitly added to the launch classpath.
    ///
    /// @return the list of library JAR files
    ///
    Stream<LibraryJarFile> extraLibraries();
}
