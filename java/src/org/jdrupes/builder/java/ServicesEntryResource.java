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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;
import org.jdrupes.builder.api.IOResource;
import org.jdrupes.builder.core.ResourceObject;

/// A temporary resource that is used to store the combined
/// `META-INF/services/` entries for a given service. The class can be
/// used for this purpose only. In particular, it does not support
/// `hashCode` or `equals`.
///
public class ServicesEntryResource extends ResourceObject
        implements IOResource {
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder content = new StringBuilder();
    private Instant asOf;

    /// Initializes a new services entry resource.
    ///
    public ServicesEntryResource() {
        // Makes javadoc happy.
    }

    @Override
    public Optional<Instant> asOf() {
        return Optional.ofNullable(asOf);
    }

    /// Adds the given resource which must be a `META-INF/services/*`
    /// entry from a jar.
    ///
    /// @param resource the resource
    /// @throws IOException Signals that an I/O exception has occurred.
    ///
    public void add(IOResource resource) throws IOException {
        try (InputStream toRead = resource.inputStream()) {
            new String(toRead.readAllBytes(), StandardCharsets.UTF_8)
                .lines().filter(Predicate.not(String::isBlank))
                .forEach(l -> content.append(l).append('\n'));
        }
        if (resource.isNewerThan(this)) {
            asOf = resource.asOf().get();
        }
    }

    @Override
    public InputStream inputStream() throws IOException {
        return new ByteArrayInputStream(
            content.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public OutputStream outputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

}
