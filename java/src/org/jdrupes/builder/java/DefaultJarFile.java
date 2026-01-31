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
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.DefaultFileResource;

/// A [FileResource] that represents a Java jar.
///
public class DefaultJarFile extends DefaultFileResource implements JarFile {

    /// Instantiates a new jar file.
    ///
    /// @param type the resource type
    /// @param path the path
    ///
    protected DefaultJarFile(ResourceType<? extends JarFile> type, Path path) {
        super(type, path);
    }
}
