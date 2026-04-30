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

/// A [FileResource] that represents a ZIP file].
///
public interface ZipFile extends FileResource {

    /// Creates a ZIP file resource from the given values.
    ///
    /// @param <T> the generic type
    /// @param jarType the jar type
    /// @param path the path
    /// @return the t
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    static <T extends ZipFile> T of(ResourceType<T> jarType, Path path) {
        return ResourceFactory.create(jarType, path);
    }

}
