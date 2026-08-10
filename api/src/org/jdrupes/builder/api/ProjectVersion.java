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

package org.jdrupes.builder.api;

import static org.jdrupes.builder.api.ResourceType.*;

/// A resource that carries the version string of a project.
///
public interface ProjectVersion extends Resource {

    /// Returns the project the version belongs to.
    ///
    /// @return the project
    ///
    Project project();

    /// Returns the version string.
    ///
    /// @return the version
    ///
    String version();

    /// Creates a new project version.
    ///
    /// @param project the project
    /// @param version the version
    /// @return the project version
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    static ProjectVersion of(Project project, String version) {
        return ResourceFactory.create(
            ProjectVersionType, project, version);
    }
}
