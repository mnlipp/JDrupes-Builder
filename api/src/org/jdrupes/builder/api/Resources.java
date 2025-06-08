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

package org.jdrupes.builder.api;

import java.time.Instant;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/// Defines a container for a collection of resources. Implementations
/// must behave as sets (no duplicate entries) and must maintain insertion
/// order when providing the content through [stream].
///
/// @param <T> the contained type
///
public interface Resources<T extends Resource> extends Resource {

    /// Adds the given resource.
    ///
    /// @param resource the resource
    /// @return the resources
    ///
    Resources<T> add(T resource);

    /// Adds all resources from the given collection.
    ///
    /// @param resources the resources to add
    /// @return the resources
    ///
    default Resources<T> addAll(Resources<? extends T> resources) {
        return addAll(resources.stream());
    }

    /// Adds all resources from the given stream.
    ///
    /// @param resources the resources to add
    /// @return the resources
    ///
    default Resources<T> addAll(Stream<? extends T> resources) {
        resources.forEach(this::add);
        return this;
    }

    /// As of.
    ///
    /// @return the instant
    ///
    @Override
    default Instant asOf() {
        return stream().map(Resource::asOf).reduce(Instant.MIN, (latest,
                next) -> next.isAfter(latest) ? next : latest);
    }

    /// Retrieves the resources as a stream.
    ///
    /// @return the stream
    ///
    Stream<T> stream();

    /// Clears the contained resources.
    ///
    /// @return the resources
    ///
    Resources<T> clear();

    /// Returns a [Collector] that accumulates resources into a new
    /// [Resources] container.
    ///
    /// @param <T> the contained resource type
    /// @param containerSupplier the container supplier
    /// @return the collector
    ///
    static <T extends Resource> Collector<T, Resources<T>, Resources<T>>
            into(Supplier<Resources<T>> containerSupplier) {
        return Collector.of(containerSupplier, Resources::add,
            Resources::addAll, Collector.Characteristics.UNORDERED);
    }
}
