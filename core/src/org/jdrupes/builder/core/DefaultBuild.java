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

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.Build.Cache.Key;
import org.jdrupes.builder.api.Resource;

/// A default implementation of a [Build].
///
public class DefaultBuild implements Build {

    private final Cache cache;
    private ExecutorService executor
        = Executors.newVirtualThreadPerTaskExecutor();

    /// Instantiates a new default build. By default, the build uses
    /// a virtual thread per task executor.
    ///
    public DefaultBuild() {
        this.cache = new DefaultCache();
    }

    /// Returns the executor service used by this build to create futures.
    ///
    /// @return the executor service
    ///
    public ExecutorService executor() {
        return executor;
    }

    /// Sets the executor service used by this build to create futures.
    ///
    /// @param executor the executor
    ///
    public void executor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public Build.Cache cache() {
        return cache;
    }

    @Override
    public <R extends Resource> Future<Optional<R>> provide(Key<R> key) {
        return cache.computeIfAbsent(key,
            k -> executor.submit(() -> k.provider().provide(k.requested())));
    }

}
