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
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;

/// Marker interface for a jar file that is a library.
///
public interface LibraryJarFile extends JarFile, ClasspathElement {

    @Override
    default Path toPath() {
        return path();
    }

    /// Creates a library jar file from the given path.
    ///
    /// @param path the path
    /// @return the library jar file
    ///
    static LibraryJarFile from(Path path) {
        return ResourceFactory.create(new ResourceType<>() {}, path);
    }
}
