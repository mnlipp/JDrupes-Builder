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

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.core.FutureStreamCache.Key;

/// A default implementation of a [Build].
///
public class DefaultBuild implements Build {

    private final FutureStreamCache cache;
    private ExecutorService executor
        = Executors.newVirtualThreadPerTaskExecutor();

    /// Instantiates a new default build. By default, the build uses
    /// a virtual thread per task executor.
    ///
    public DefaultBuild() {
        cache = new FutureStreamCache();
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
    public <T extends Resource> Stream<T> provide(ResourceProvider<T> provider,
            Resource requested) {
        return cache.computeIfAbsent(new Key<>(provider, requested),
            k -> new FutureStream<T>(executor, k.provider(), k.requested()))
            .stream();
    }
}
