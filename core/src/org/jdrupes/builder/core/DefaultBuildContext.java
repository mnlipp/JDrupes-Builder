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
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.jdrupes.builder.api.BuildContext;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.core.FutureStreamCache.Key;

/// A context for building.
///
public class DefaultBuildContext implements BuildContext {

    /// The key for specifying the builder directory in the properties file.
    public static final String JDBLD_DIRECTORY = "jdbldDirectory";
    private final FutureStreamCache cache;
    private ExecutorService executor
        = Executors.newVirtualThreadPerTaskExecutor();

    /// Instantiates a new default build. By default, the build uses
    /// a virtual thread per task executor.
    ///
    /* default */ DefaultBuildContext() {
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
    public <T extends Resource> Stream<T> get(ResourceProvider provider,
            ResourceRequest<T> request) {
        if (provider instanceof Project project) {
            var defReq = (DefaultResourceRequest<T>) request;
            if (Arrays.asList(defReq.queried()).contains(provider)) {
                return Stream.empty();
            }
            request = defReq.queried(project);
        }
        return cache.computeIfAbsent(new Key<>(provider, request),
            k -> new FutureStream<T>(executor, k.provider(), k.request()))
            .stream();
    }

    @Override
    public Path jdbldDirectory() {
        return Path
            .of(LauncherSupport.jdbldProperties().getProperty(JDBLD_DIRECTORY));
    }

    @Override
    public CommandLine commandLine() {
        return LauncherSupport.commandLine();
    }

    @Override
    public String property(String name) {
        return LauncherSupport.jdbldProperties().getProperty(name);
    }

}
