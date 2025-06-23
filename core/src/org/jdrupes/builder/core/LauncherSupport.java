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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.RootProject;

/// Provides support for creating projects based on [AbstractProject].
///
public final class LauncherSupport {

    private static Properties jdbldProps;
    private static String[] commandArgs;

    private LauncherSupport() {
    }

    /// Creates and initializes the root project and the sub projects.
    /// Adds the sub projects to the root project automatically. This
    /// method should be used if the launcher detects the sub projects
    /// e.g. by reflection and the root project does not add its sub
    /// projects itself.
    ///
    /// @param rootProject the root project
    /// @param subprojects the sub projects
    /// @param jdbldProps the builder properties
    /// @param args the arg list
    /// @return the root project
    ///
    @SuppressWarnings("PMD.UseVarargs")
    public static RootProject createProjects(
            Class<? extends RootProject> rootProject,
            List<Class<? extends Project>> subprojects,
            Properties jdbldProps, String[] args) {
        try {
            LauncherSupport.jdbldProps = jdbldProps;
            scanArgs(args);
            var result = rootProject.getConstructor().newInstance();
            subprojects.forEach(result::project);
            return result;
        } catch (SecurityException | NegativeArraySizeException
                | IllegalArgumentException | InstantiationException
                | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /* default */ static Properties jdbldProperties() {
        return jdbldProps;
    }

    @SuppressWarnings("PMD.UseVarargs")
    private static void scanArgs(String[] args) {
        var bootstrapArgs = new ArrayList<String>();
        var itr = Arrays.asList(args).iterator();
        while (itr.hasNext()) {
            var arg = itr.next();
            if (arg.startsWith("-B-x") && itr.hasNext()) {
                bootstrapArgs.add("-x");
                bootstrapArgs.add(itr.next());
            }
        }
        commandArgs = bootstrapArgs.toArray(new String[0]);
    }

    /* default */ @SuppressWarnings("PMD.MethodReturnsInternalArray")
    static String[] commandArgs() {
        return commandArgs;
    }

    /// Lookup the command in the given root project.
    ///
    /// @param project the project
    /// @param name the name
    /// @return the optional
    ///
    public static ResourceRequest<?>[]
            lookupCommand(RootProject project, String name) {
        return ((AbstractProject) project).lookupCommand(name);
    }
}
