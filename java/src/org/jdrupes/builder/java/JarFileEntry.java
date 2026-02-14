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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.jdrupes.builder.api.IOResource;
import org.jdrupes.builder.core.ResourceObject;

/// Represents an entry in a jar file.
///
public class JarFileEntry extends ResourceObject implements IOResource {

    private final JarFile jarFile;
    private final JarEntry entry;

    /// Initializes a new jar file entry.
    ///
    /// @param jarFile the jar file
    /// @param entry the entry
    ///
    public JarFileEntry(JarFile jarFile, JarEntry entry) {
        super();
        this.jarFile = jarFile;
        this.entry = entry;
    }

    @Override
    public Optional<Instant> asOf() {
        return Optional.of(entry.getLastModifiedTime().toInstant());
    }

    @Override
    public InputStream inputStream() throws IOException {
        return jarFile.getInputStream(entry);
    }

    @Override
    public OutputStream outputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result
            = prime * result + Objects.hash(entry.getName(), jarFile.getName());
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
        return (obj instanceof JarFileEntry other)
            && Objects.equals(entry.getName(), other.entry.getName())
            && Objects.equals(jarFile.getName(), other.jarFile.getName());
    }

    @Override
    public String toString() {
        return jarFile.getName() + "!" + entry.getName() + " ("
            + asOfLocalized() + ")";
    }

}
