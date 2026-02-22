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
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.jdrupes.builder.api.BuildContext;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Intent;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceType.CleanlinessType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.api.StatusLine;
import org.jdrupes.builder.core.FutureStreamCache.Key;
import org.jdrupes.builder.core.console.SplitConsole;

/// A context for building.
///
public class DefaultBuildContext implements BuildContext {

    /// The key for specifying the builder directory in the properties file.
    public static final String JDBLD_DIRECTORY = "jdbldDirectory";
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final ScopedValue<AtomicBoolean> providerInvocationAllowed
        = ScopedValue.newInstance();
    private final FutureStreamCache cache;
    private ExecutorService executor
        = Executors.newVirtualThreadPerTaskExecutor();
    private final Path buildRoot;
    private final Properties jdbldProperties;
    private final CommandLine commandLine;
    private final AwaitableCounter executingFutureStreams
        = new AwaitableCounter();
    private final SplitConsole console;
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final ScopedValue<
            DefaultBuildContext> scopedBuildContext = ScopedValue.newInstance();

    /// Instantiates a new default build. By default, the build uses
    /// a virtual thread per task executor.
    ///
    /* default */ DefaultBuildContext(Path buildRoot,
            Properties jdbldProperties, CommandLine commandLine) {
        this.buildRoot = buildRoot;
        this.jdbldProperties = jdbldProperties;
        this.commandLine = commandLine;
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

    /// Executing future streams.
    ///
    /// @return the awaitable counter
    ///
    public AwaitableCounter executingFutureStreams() {
        return executingFutureStreams;
    }

    /// Returns the build root.
    ///
    /// @return the path
    ///
    public Path buildRoot() {
        return buildRoot;
    }

    @Override
    public Path jdbldDirectory() {
        return Path.of(jdbldProperties.getProperty(JDBLD_DIRECTORY));
    }

    @Override
    public CommandLine commandLine() {
        return commandLine;
    }

    @Override
    public String property(String name, String defaultValue) {
        return jdbldProperties.getProperty(name,
            defaultValue);
    }

    /// Returns the context.
    ///
    /// @return the optional
    ///
    public static Optional<DefaultBuildContext> context() {
        if (scopedBuildContext.isBound()) {
            return Optional.of(scopedBuildContext.get());
        }
        return Optional.empty();
    }

    /// Call within this context. 
    ///
    /// @param <T> the generic type
    /// @param supplier the supplier
    /// @return the t
    ///
    public <T> T call(Supplier<T> supplier) {
        return ScopedValue.where(scopedBuildContext, this).call(supplier::get);
    }

    /* default */ SplitConsole console() {
        return console;
    }

    @Override
    public StatusLine statusLine() {
        return FutureStream.statusLine.orElse(SplitConsole.nullStatusLine());
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
        return ScopedValue.where(scopedBuildContext, this)
            .where(providerInvocationAllowed, new AtomicBoolean(true))
            .call(() -> inResourcesContext(provider, requested));
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private <T extends Resource> Stream<T> inResourcesContext(
            ResourceProvider provider, ResourceRequest<T> requested) {
        if (provider instanceof Project project) {
            var defReq = (DefaultResourceRequest<T>) requested;
            if (Arrays.asList(defReq.queried()).contains(provider)) {
                return Stream.empty();
            }
            // Log invocation with request
            var req = defReq.queried(project);
            // As a project's provide only delegates to other providers
            // it is inefficient to invoke it asynchronously. Besides, it
            // leads to recursive invocations of the project's deploy
            // method too easily and results in a loop detection without
            // there really being a loop.
            return ((AbstractProvider) provider).toSpi().provide(req);
        }
        var req = requested;
        if (!req.uses().isEmpty()) {
            req = requested.using(EnumSet.noneOf(Intent.class));
        }
        if (!requested.type().equals(CleanlinessType)) {
            return cache.computeIfAbsent(new Key<>(provider, req),
                k -> new FutureStream<T>(this, scopedBuildContext,
                    providerInvocationAllowed, k.provider(), k.request()))
                .stream();
        }

        // Special handling for cleanliness. Clean one by one...
        synchronized (executor) {
            // Await completion of all generating threads
            try {
                executingFutureStreams().await(0);
            } catch (InterruptedException e) {
                throw new BuildException().cause(e);
            }
        }
        var result = ((AbstractProvider) provider).toSpi().provide(requested);
        // Purge cached results from provider
        cache.purge(provider);
        return result;
    }

    /// Checks if is provider invocation is allowed. Clears the
    /// allowed flag to also detect nested invocations.
    ///
    /// @return true, if is provider invocation allowed
    ///
    public static boolean isProviderInvocationAllowed() {
        return providerInvocationAllowed.isBound()
            && providerInvocationAllowed.get().getAndSet(false);
    }

    @Override
    public void close() {
        executor.shutdownNow();
        console.close();
    }

    /// Creates and initializes the root project and the sub projects.
    /// Adds the sub projects to the root project automatically. This
    /// method should be used if the launcher detects the sub projects
    /// e.g. by reflection and the root project does not add its sub
    /// projects itself.
    ///
    /// @param buildRoot the build root
    /// @param rootProject the root project
    /// @param subprojects the sub projects
    /// @param jdbldProps the builder properties
    /// @param commandLine the command line
    /// @return the root project
    ///
    public static AbstractRootProject createProjects(
            Path buildRoot, Class<? extends RootProject> rootProject,
            List<Class<? extends Project>> subprojects,
            Properties jdbldProps, CommandLine commandLine) {
        try {
            return ScopedValue
                .where(scopedBuildContext,
                    new DefaultBuildContext(buildRoot, jdbldProps, commandLine))
                .call(() -> {
                    var result = (AbstractRootProject) rootProject
                        .getConstructor().newInstance();
                    result.unlockProviders();
                    subprojects.forEach(result::project);
                    return result;

                });
        } catch (SecurityException | NegativeArraySizeException
                | IllegalArgumentException | ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
