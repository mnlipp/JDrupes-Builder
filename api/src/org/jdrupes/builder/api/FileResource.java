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

package org.jdrupes.builder.api;

import java.nio.file.Path;

/// A resource that represents a file.
///
public interface FileResource extends IOResource {

    /// Return the file's path.
    ///
    /// @return the path
    ///
    Path path();

    /// Cleans up by deleting the file.
    ///
    @Override
    default void cleanup() {
        path().toFile().delete();
    }

    /// Convenience method for creating a file resource.
    ///
    /// @param project the project
    /// @param path the path relative to the project's directory
    /// @return the file resource
    ///
    @SuppressWarnings("PMD.UseDiamondOperator")
    static FileResource create(Project project, Path path) {
        return project.newResource(new ResourceType<FileResource>() {},
            project.directory().resolve(path));
    }
}