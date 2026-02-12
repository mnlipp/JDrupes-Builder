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

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.jdrupes.builder.api.BuildContext;
import org.jdrupes.builder.api.Intent;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.StatusLine;
import org.jdrupes.builder.core.FutureStreamCache.Key;
import org.jdrupes.builder.core.console.SplitConsole;

/// A context for building.
///
public class DefaultBuildContext implements BuildContext {

    /// The key for specifying the builder directory in the properties file.
    public static final String JDBLD_DIRECTORY = "jdbldDirectory";
    private final FutureStreamCache cache;
    private ExecutorService executor
        = Executors.newVirtualThreadPerTaskExecutor();
    private final SplitConsole console;

    /// Instantiates a new default build. By default, the build uses
    /// a virtual thread per task executor.
    ///
    /* default */ DefaultBuildContext() {
        cache = new FutureStreamCache();
        console = SplitConsole.open();
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

    /* default */ SplitConsole console() {
        return console;
    }

    @Override
    public Optional<StatusLine> statusLine() {
        if (!FutureStream.statusLine.isBound()) {
            return Optional.empty();
        }
        return Optional.of(FutureStream.statusLine.get());
    }

    @Override
    public PrintStream out() {
        return console().out();
    }

    @Override
    public PrintStream error() {
        return console().err();
    }

    @Override
    public <T extends Resource> Stream<T> resources(ResourceProvider provider,
            ResourceRequest<T> requested) {
        if (provider instanceof Project project) {
            var defReq = (DefaultResourceRequest<T>) requested;
            if (Arrays.asList(defReq.queried()).contains(provider)) {
                return Stream.empty();
            }
            // Log invocation
            requested = defReq.queried(project);
            // As a project's provide only delegates to other providers
            // it is inefficient to invoke it asynchronously. Besides, it
            // leads to recursive invocations of the project's deploy
            // method too easily and results in a loop detection without
            // there really being a loop.
            return ((AbstractProvider) provider).doProvide(requested);
        }
        var req = requested;
        if (!req.uses().isEmpty()) {
            req = requested.using(EnumSet.noneOf(Intent.class));
        }
        return cache.computeIfAbsent(new Key<>(provider, req),
            k -> new FutureStream<T>(this, k.provider(), k.request()))
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
    public String property(String name, String defaultValue) {
        return LauncherSupport.jdbldProperties().getProperty(name,
            defaultValue);
    }

    @Override
    public void close() {
        executor.shutdownNow();
        console.close();
    }
}
