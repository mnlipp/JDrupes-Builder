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

package org.jdrupes.builder.java;

import java.nio.file.Path;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;

/// A [FileResource] that represents a Java jar file].
///
public interface JarFile extends FileResource {

    /// Creates a jar file resource from the given values.
    ///
    /// @param <T> the generic type
    /// @param jarType the jar type
    /// @param path the path
    /// @return the t
    ///
    static <T extends JarFile> T from(ResourceType<T> jarType, Path path) {
        return ResourceFactory.create(jarType, path);
    }

}
