package org.jdrupes.builder.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.Build.Cache.Key;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;

public class DefaultBuild implements Build {

    private Cache cache;
    private ExecutorService executor
        = Executors.newVirtualThreadPerTaskExecutor();

    public DefaultBuild() {
        this.cache = new DefaultCache();
    }

    public Build.Cache cache() {
        return cache;
    }

    @Override
    public <R extends Resource> Resources<R> provide(Key<R> key) {
        return cache.computeIfAbsent(key,
            k -> new FutureResources<>(executor, k.provider(), k.requested()));
    }

}
