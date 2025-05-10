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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;

/// Represents a set of resources.
///
public class ResourceSet<R extends Resource> implements Resources<R> {

    protected final Logger log = Logger.getLogger(getClass().getName());

    protected final Set<R> content;

    public ResourceSet() {
        content = new HashSet<>();
    }

    public ResourceSet(Collection<R> content) {
        this.content = new HashSet<>(content);
    }

    @SafeVarargs
    public static <R extends Resource> Resources<R> of(R... resources) {
        return new ResourceSet<>(Arrays.asList(resources));
    }

    /// Returns when the resources were last modified.
    /// 
    /// @return the instant
    ///
    public Instant asOf() {
        return Instant.MIN;
    }

    @Override
    public Resources<R> add(R resource) {
        content.add(resource);
        return this;
    }

    @Override
    public Stream<R> stream() {
        return content.stream();
    }

}
