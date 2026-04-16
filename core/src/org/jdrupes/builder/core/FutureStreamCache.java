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

/// An implementation of a cache for [FutureStream]s.
///
public class FutureStreamCache {

    private final Map<ProviderInvocation<? extends Resource>,
            FutureStream<? extends Resource>> cache = new ConcurrentHashMap<>();

    /// Instantiates a new future stream cache.
    ///
    /* default */ FutureStreamCache() {
        // Make javadoc happy
    }

    /// Compute if absent.
    ///
    /// @param <T> the generic type
    /// @param key the key
    /// @param supplier the supplier
    /// @return the future stream
    ///
    @SuppressWarnings("unchecked")
    public <T extends Resource> FutureStream<T> computeIfAbsent(ProviderInvocation<T> key,
            Function<ProviderInvocation<T>, FutureStream<T>> supplier) {
        // This is actually type-safe, because the methods for entering
        // key value pairs allow only values typed according to the casts
        return (FutureStream<T>) cache.computeIfAbsent(key,
            (Function<ProviderInvocation<? extends Resource>,
                    FutureStream<? extends Resource>>) (Object) supplier);
    }

    /// Purge all values for the given [ResourceProvider].
    ///
    /// @param provider the provider
    ///
    public void purge(ResourceProvider provider) {
        cache.forEach((k, _) -> {
            if (k.provider().equals(provider)) {
                cache.remove(k);
            }
        });
    }

}
