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

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.RootProject;

/// Provides support for creating projects based on [AbstractProject].
///
public final class LauncherSupport {

    private LauncherSupport() {
    }

    /// Creates and initializes a root project. This method must be called
    /// if the root project creates its sub projects itself.
    ///
    /// @param rootProject the root project
    ///
    public static RootProject
            createRootProject(Class<? extends RootProject> rootProject) {
        try {
            var result = rootProject.getConstructor().newInstance();
            ((AbstractProject) result).createProjects();
            return result;
        } catch (NoSuchMethodException | SecurityException
                | NegativeArraySizeException | InstantiationException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /// Creates and initializes the root project and the sub projects.
    /// Adds the sub projects to the root project automatically. This
    /// method should be used if the launcher detects the sub projects
    /// e.g. by reflection and the root project does not add its sub
    /// projects itself.
    ///
    /// @param rootProject the root project
    /// @param subprojects the sub projects
    /// @return the root project
    ///
    public static RootProject createProjects(
            Class<? extends RootProject> rootProject,
            List<Class<? extends Project>> subprojects) {
        try {
            AbstractProject.detectedSubprojects(subprojects);
            var result = rootProject.getConstructor().newInstance();
            ((AbstractProject) result).createProjects();
            return result;
        } catch (SecurityException | NegativeArraySizeException
                | IllegalArgumentException | InstantiationException
                | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
