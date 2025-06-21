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

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.Proxyable;
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
    protected DefaultFileResource(ResourceType<? extends FileResource> type,
            Path path) {
        super(type);
        if (!path.isAbsolute()) {
            throw new BuildException("Path must be absolute, is " + path);
        }
        this.path = path;
    }

    /// Creates a new file resource.
    ///
    /// @param <T> the resource type
    /// @param type the type
    /// @param path the path
    /// @return the t
    ///
    @SuppressWarnings({ "unchecked" })
    public static <T extends FileResource> T createFileResource(
            ResourceType<T> type, Path path) {
        return (T) Proxy.newProxyInstance(type.rawType().getClassLoader(),
            new Class<?>[] { type.rawType(), Proxyable.class },
            new ForwardingHandler(new DefaultFileResource(type, path)));
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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(path, type());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        DefaultFileResource other = (DefaultFileResource) obj;
        return Objects.equals(path, other.path)
            && Objects.equals(type(), other.type());
    }

    @Override
    public String toString() {
        var relPath = Path.of("").toAbsolutePath().relativize(path);
        return type() + ": " + relPath.toString()
            + " (" + asOfLocalized() + ")";
    }
}
