package org.jdrupes.builder.core;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.Build.Cache.Key;
import org.jdrupes.builder.api.Provider;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;

public class DefaultCache implements Build.Cache {

    private Map<Key<? extends Resource>, Resources<? extends Resource>> cache
        = new ConcurrentHashMap<>();

    @Override
    public <R extends Resource> void put(Key<R> key, Resources<R> value) {
        cache.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Resource> Optional<Resources<R>> get(Key<R> key) {
        // This is actually type-safe, because the methods for entering
        // key value pairs allow only values typed according to the cast
        return Optional.ofNullable((Resources<R>) cache.get(key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Resource> Resources<R> computeIfAbsent(Key<R> key,
            Function<Key<R>, Resources<R>> supplier) {
        // This is actually type-safe, because the methods for entering
        // key value pairs allow only values typed according to the casts
        return (Resources<R>) cache.computeIfAbsent(key,
            (Function<Key<? extends Resource>,
                    Resources<? extends Resource>>) (Object) supplier);
    }

    @Override
    public void remove(Key key) {
        cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

}
