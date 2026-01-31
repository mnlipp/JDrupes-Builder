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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.ResourceType;

/// A resource that represents a file.
///
public class DefaultFileResource extends ResourceObject
        implements FileResource {

    private final Path path;

    /// Instantiates a new file resource.
    ///
    /// @param type the type
    /// @param path the path
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    protected DefaultFileResource(ResourceType<? extends FileResource> type,
            Path path) {
        super(type);
        if (!path.isAbsolute()) {
            throw new BuildException("Path must be absolute, is " + path);
        }
        var relPath = Path.of("").toAbsolutePath().relativize(path);
        name(relPath.equals(Path.of("")) ? "." : relPath.toString());
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
        if (!path.toFile().exists()) {
            return Instant.MIN;
        }
        return Instant.ofEpochMilli(path.toFile().lastModified());
    }

    @Override
    public InputStream inputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public OutputStream outputStream() throws IOException {
        return Files.newOutputStream(path);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(path);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        return (obj instanceof FileResource other)
            && Objects.equals(path, other.path());
    }
}
