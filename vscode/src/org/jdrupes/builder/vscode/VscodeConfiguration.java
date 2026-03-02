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

package org.jdrupes.builder.vscode;

import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;

/// A resource request for the VSCode configuration files. The request
/// results in the configured project's directory.
///
public interface VscodeConfiguration extends FileResource {

    /// Creates a new VscodeConfiguration from the given values.
    ///
    /// @param project the project
    /// @return the eclipse configuration
    ///
    static VscodeConfiguration from(Project project) {
        return ResourceFactory.create(new ResourceType<>() {},
            project, project.directory());
    }

}
