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

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.jdrupes.builder.api.Build.Cache.Key;

/// A build is the collection of all data related to building all projects.
/// In particular, it holds the global configuration information for the
/// build, as well as a cache of all resources that have been requested
/// from the [ResourceProvider]s during the build.
/// 
public interface Build {

    /// Defines a cache of resources (as [Future]s).
    ///
    interface Cache {

        /// Provided resources are identified by the [ResourceProvider]
        /// and the requested [Resource].
        ///
        record Key<T extends Resource>(ResourceProvider<T> provider,
                Resource requested) {
        }

        /// Puts a key-value pair into the cache.
        ///
        /// @param <T> the type of resource
        /// @param key the key
        /// @param value the value
        ///
        <T extends Resource> void put(Key<T> key, Future<Optional<T>> value);

        /// Gets the value for the given key from the cache.
        ///
        /// @param <T> the type of resource
        /// @param key the key
        /// @return the associated value
        ///
        <T extends Resource> Optional<Future<Optional<T>>> get(Key<T> key);

        /// Attempts to compute a value for the given key if it is not
        /// already present in the cache.
        ///
        /// @param <T> the type of resource
        /// @param key the key
        /// @param supplier the supplier
        /// @return the resource
        ///
        <T extends Resource> Future<Optional<T>> computeIfAbsent(Key<T> key,
                Function<Key<T>, Future<Optional<T>>> supplier);

        /// Removes the entry with the given key.
        ///
        /// @param key the key
        ///
        <T extends Resource> void remove(Key<T> key);

        /// Clears the cache.
        ///
        void clear();

    }

    /// Returns the cache for the build.
    ///
    /// @return the cache
    ///
    Cache cache();

    /// Start the asynchronous evaluation of the requested resource.
    ///
    /// @param <T> the type of resource
    /// @param key the key
    /// @return the resource
    ///
    <T extends Resource> Future<Optional<T>> provide(Key<T> key);

    /// Short for `provide(new Key<>(provider, requested))`.
    ///
    /// @param <T> the type of resource
    /// @param provider the provider
    /// @param requested the requested
    /// @return the resource
    ///
    default <T extends Resource> Future<Optional<T>> provide(
            ResourceProvider<T> provider, Resource requested) {
        return provide(new Key<>(provider, requested));
    }
}
