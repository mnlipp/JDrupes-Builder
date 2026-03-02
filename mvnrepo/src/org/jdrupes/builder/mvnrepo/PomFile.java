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

package org.jdrupes.builder.mvnrepo;

import java.nio.file.Path;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;

/// A [FileResource] that represents a POM file.
///
public interface PomFile extends FileResource {

    /// Creates a new POM file resource from the given path.
    ///
    /// @param path the path
    /// @return the POM file
    ///
    static PomFile from(Path path) {
        return ResourceFactory.create(new ResourceType<>() {}, path);
    }

}
