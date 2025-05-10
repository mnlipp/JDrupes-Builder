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

public interface Build {

    public interface Cache {

        static record Key<R extends Resource>(Provider<R> provider,
                Resource requested) {
        }

        <R extends Resource> void put(Key<R> key, Resources<R> value);

        <R extends Resource> Optional<Resources<R>> get(Key<R> key);

        <R extends Resource> Resources<R> computeIfAbsent(Key<R> key,
                Function<Key<R>, Resources<R>> supplier);

        void remove(Key key);

        void clear();

    }

    Cache cache();

    /// Provides the resources for the given key from the given provider.
    /// The result from invoking the provider is evaluated asynchronously
    /// and cached. Only when [Resources#asOf] is called or the stream
    /// from [Resources#stream] is terminated will the invocation block
    /// until the result from the provider is available.
    ///
    /// @param <R> the generic type
    /// @param key the key
    /// @return the resources
    ///
    <R extends Resource> Resources<R> provide(Key<R> key);

    /// Short for `provide(new Key<>(provider, requested))`.
    ///
    /// @param <R> the generic type
    /// @param provider the provider
    /// @param requested the requested
    /// @return the resources
    ///
    ///
    default <R extends Resource> Resources<R> provide(Provider<R> provider,
            Resource requested) {
        return provide(new Key<>(provider, requested));
    }
}
