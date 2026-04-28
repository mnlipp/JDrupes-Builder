/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
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

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.jdrupes.builder.api.ConfigurationException;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.RootProject;

/// The Class LauncherBase.
///
@SuppressWarnings("PMD.UseUtilityClass")
public class LauncherBase {

    /// The ScopedValue for the build context.
    @SuppressWarnings("PMD.FieldNamingConventions")
    protected static final ScopedValue<
            DefaultBuildContext> scopedBuildContext = ScopedValue.newInstance();

    static {
        ScopedValueContext.add(scopedBuildContext);
    }

    /// Initializes a new launcher base.
    ///
    public LauncherBase() {
        // Make javadoc happy
    }

    /// Returns the context.
    ///
    /// @return the optional
    ///
    public static DefaultBuildContext context() {
        if (!scopedBuildContext.isBound()) {
            throw new ConfigurationException()
                .cause(new IllegalStateException())
                .message("BuildContext is not bound.");
        }
        return scopedBuildContext.get();
    }

    /// Creates and initializes the root project and the sub projects.
    /// Adds the sub projects to the root project automatically. This
    /// method should be used if the launcher detects the sub projects
    /// e.g. by reflection and the root project does not add its sub
    /// projects itself.
    ///
    /// @param buildRoot the build root
    /// @param rootProject the root project
    /// @param subprojects the sub projects
    /// @param jdbldProps the builder properties
    /// @param commandLine the command line
    /// @return the root project
    ///
    protected static AbstractRootProject createProjects(
            Path buildRoot, Class<? extends RootProject> rootProject,
            List<Class<? extends Project>> subprojects,
            Properties jdbldProps, CommandLine commandLine) {
        try {
            return ScopedValue
                .where(scopedBuildContext,
                    new DefaultBuildContext(buildRoot, jdbldProps, commandLine))
                .call(() -> {
                    var result = (AbstractRootProject) rootProject
                        .getConstructor().newInstance();
                    result.unlockProviders();
                    subprojects.forEach(result::project);
                    scopedBuildContext.get().buildProject().complete(result);
                    return result;
                });
        } catch (SecurityException | NegativeArraySizeException
                | IllegalArgumentException | ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
