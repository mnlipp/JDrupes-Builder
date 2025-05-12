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

/// A default implementation of a [Cache].
///
public class FutureStreamCache {

    private final Map<Key<? extends Resource>,
            FutureStream<? extends Resource>> cache = new ConcurrentHashMap<>();

    /// Provided resources are identified by the [ResourceProvider]
    /// and the requested [Resource].
    ///
    public record Key<T extends Resource>(ResourceProvider<T> provider,
            Resource requested) {
    }

    @SuppressWarnings("unchecked")
    public <R extends Resource> FutureStream<R> computeIfAbsent(Key<R> key,
            Function<Key<R>, FutureStream<R>> supplier) {
        // This is actually type-safe, because the methods for entering
        // key value pairs allow only values typed according to the casts
        return (FutureStream<R>) cache.computeIfAbsent(key,
            (Function<Key<? extends Resource>,
                    FutureStream<? extends Resource>>) (Object) supplier);
    }

}
