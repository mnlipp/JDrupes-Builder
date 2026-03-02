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

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;

/// A resource request for the Eclipse configuration files. The request
/// results in information about the Eclipse configuration.
///
public interface EclipseConfiguration extends Resource {

    /// The jdbld project name.
    ///
    /// @return the string
    ///
    String projectName();

    /// The project name used in the Eclipse configuration.
    ///
    /// @return the string
    ///
    String eclipseAlias();

    /// Creates a new EclipseConfiguration from the given values.
    ///
    /// @param project the project
    /// @param eclipseAlias the eclipse alias
    /// @return the eclipse configuration
    ///
    static EclipseConfiguration from(Project project, String eclipseAlias) {
        return ResourceFactory.create(new ResourceType<>() {},
            project, project.name(), eclipseAlias);
    }
}
