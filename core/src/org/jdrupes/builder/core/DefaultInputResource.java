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

package org.jdrupes.builder.core;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import org.jdrupes.builder.api.InputResource;
import org.jdrupes.builder.api.ResourceType;

/// The Class DefaultInputResource.
///
public class DefaultInputResource extends ResourceObject
        implements InputResource {

    private final Instant asOf;
    private final InputStream inputStream;

    /// Initializes a new default input resource.
    ///
    /// @param type the type
    /// @param asOf the as of
    /// @param inputStream the input stream
    ///
    public DefaultInputResource(ResourceType<? extends InputResource> type,
            Instant asOf, InputStream inputStream) {
        super(type);
        this.asOf = asOf;
        this.inputStream = inputStream;
    }

    @Override
    public Optional<Instant> asOf() {
        return Optional.of(asOf);
    }

    @Override
    public InputStream inputStream() {
        return inputStream;
    }
}
