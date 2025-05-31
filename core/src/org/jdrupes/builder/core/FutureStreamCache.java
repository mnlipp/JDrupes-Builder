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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;

/// An implementation of a cache for [FutureStream]s.
///
public class FutureStreamCache {

    private final Map<Key<? extends Resource>,
            FutureStream<? extends Resource>> cache = new ConcurrentHashMap<>();

    /// Provided resources are identified by the [ResourceProvider]
    /// and the requested [Resource].
    ///
    /// @param <T> the generic type
    /// @param provider the provider
    /// @param requested the requested resources
    ///
    public record Key<T extends Resource>(ResourceProvider<?> provider,
            ResourceRequest<T> requested) {
    }

    /// Compute if absent.
    ///
    /// @param <T> the generic type
    /// @param key the key
    /// @param supplier the supplier
    /// @return the future stream
    ///
    @SuppressWarnings("unchecked")
    public <T extends Resource> FutureStream<T> computeIfAbsent(Key<T> key,
            Function<Key<T>, FutureStream<T>> supplier) {
        // This is actually type-safe, because the methods for entering
        // key value pairs allow only values typed according to the casts
        return (FutureStream<T>) cache.computeIfAbsent(key,
            (Function<Key<? extends Resource>,
                    FutureStream<? extends Resource>>) (Object) supplier);
    }

}
