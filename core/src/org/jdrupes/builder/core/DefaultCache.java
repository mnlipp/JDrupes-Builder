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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.Resource;

/// A default implementation of a [Cache].
///
public class DefaultCache implements Build.Cache {

    private final Map<Key<? extends Resource>,
            Future<Optional<? extends Resource>>> cache
                = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Resource> void put(Key<R> key,
            Future<Optional<R>> value) {
        cache.put(key, (Future<Optional<? extends Resource>>) (Object) value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Resource> Optional<Future<Optional<T>>>
            get(Key<T> key) {
        // This is actually type-safe, because the methods for entering
        // key value pairs allow only values typed according to the cast
        return Optional
            .ofNullable((Future<Optional<T>>) (Object) cache.get(key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Resource> Future<Optional<T>> computeIfAbsent(Key<T> key,
            Function<Key<T>, Future<Optional<T>>> supplier) {
        // This is actually type-safe, because the methods for entering
        // key value pairs allow only values typed according to the casts
        return (Future<Optional<T>>) (Object) cache.computeIfAbsent(key,
            (Function<Key<? extends Resource>,
                    Future<Optional<? extends Resource>>>) (Object) supplier);
    }

    @Override
    public <T extends Resource> void remove(Key<T> key) {
        cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

}
