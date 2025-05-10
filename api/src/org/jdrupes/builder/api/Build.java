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
import java.util.function.Function;
import org.jdrupes.builder.api.Build.Cache.Key;

/// A build is the collection of all data related to building all projects.
/// In particular, it holds the global configuration information for the
/// build, as well as a cache of all resources that have been requested
/// from the [Provider]s during the build.
/// 
public interface Build {

    /// Defines a cache of resources.
    ///
    interface Cache {

        /// Resources are identified by a [Provider] and the 
        /// requested [Resource].
        ///
        record Key<T extends Resource>(Provider<T> provider,
                Resource requested) {
        }

        /// Puts a key-value pair into the cache.
        ///
        /// @param <T> the generic type
        /// @param key the key
        /// @param value the value
        ///
        <T extends Resource> void put(Key<T> key, Resources<T> value);

        /// Gets the value for the given key from the cache.
        ///
        /// @param <T> the generic type
        /// @param key the key
        /// @return the associated value
        ///
        <T extends Resource> Optional<Resources<T>> get(Key<T> key);

        /// Attempts to compute a value for the given key if it is not
        /// already present in the cache.
        ///
        /// @param <T> the generic type
        /// @param key the key
        /// @param supplier the supplier
        /// @return the resources
        ///
        <T extends Resource> Resources<T> computeIfAbsent(Key<T> key,
                Function<Key<T>, Resources<T>> supplier);

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

    /// Provides the resources for the given key from the given provider.
    /// The result from invoking the provider is evaluated asynchronously
    /// and cached. Only when [Resources#asOf] is called or the stream
    /// from [Resources#stream] is terminated will the invocation block
    /// until the result from the provider becomes available.
    ///
    /// @param <T> the generic type
    /// @param key the key
    /// @return the resources
    ///
    <T extends Resource> Resources<T> provide(Key<T> key);

    /// Short for `provide(new Key<>(provider, requested))`.
    ///
    /// @param <T> the generic type
    /// @param provider the provider
    /// @param requested the requested
    /// @return the resources
    ///
    default <T extends Resource> Resources<T> provide(Provider<T> provider,
            Resource requested) {
        return provide(new Key<>(provider, requested));
    }
}
