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

import java.time.Instant;
import java.util.Optional;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.VirtualResource;

/// An implementation of [VirtualResource].
///
public class DefaultVirtualResource extends ResourceObject
        implements VirtualResource {

    private final Instant createdAt = Instant.now();

    /// Initializes a new default virtual resource.
    ///
    /// @param type the type
    ///
    public DefaultVirtualResource(ResourceType<?> type) {
        super(type);
    }

    @Override
    public Optional<Instant> asOf() {
        return Optional.of(createdAt);
    }

}
