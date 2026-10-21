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

package org.jdrupes.builder.eclipse;

import java.nio.file.Path;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;

/// A resource representing an Eclipse launch configuration file.
///
public interface EclipseLauncher extends FileResource {

    /// Creates a new Eclipse launch configuration from the given values.
    ///
    /// @param path the path
    /// @return the Eclipse launch configuration file
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    static EclipseLauncher of(Path path) {
        return ResourceFactory.create(new ResourceType<>() {}, path);
    }
}
