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

package org.jdrupes.builder.core;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.Resource;

/// A resource that represents a file.
///
public class DefaultFileResource implements Resource, FileResource {

    private final Path path;

    /// Instantiates a new file resource.
    ///
    /// @param path the path
    ///
    public DefaultFileResource(Path path) {
        this.path = path;
    }

    /// Path.
    ///
    /// @return the path
    ///
    @Override
    public Path path() {
        return path;
    }

    @Override
    public Instant asOf() {
        return Instant.ofEpochMilli(path.toFile().lastModified());
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof FileResource)) {
            return false;
        }
        FileResource other = (FileResource) obj;
        return Objects.equals(path, other.path());
    }

    @Override
    public String toString() {
        var relPath = Path.of("").toAbsolutePath().relativize(path);
        return "File: " + relPath.toString() + " (" + asOfLocalized() + ")";
    }
}
