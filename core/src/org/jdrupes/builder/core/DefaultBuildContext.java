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

import com.google.common.flogger.FluentLogger;
import static com.google.common.flogger.LazyArgs.lazy;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
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
import org.jdrupes.builder.core.console.SplitConsole;

/// A context for building.
///
@SuppressWarnings({ "PMD.CouplingBetweenObjects" })
public class DefaultBuildContext implements BuildContext {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
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
    private final CompletableFuture<AbstractRootProject> buildProject
        = new CompletableFuture<>();
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final ScopedValue<RequestChainLink> requestChainEnd
        = ScopedValue.newInstance();

    static {
        ScopedValueContext.add(scopedBuildContext, requestChainEnd);
    }

    /// A link in the call chain.
    ///
    /// @param previous the previous
    /// @param invocation the invocation
    ///
    public record RequestChainLink(RequestChainLink previous,
            ProviderInvocation<?> invocation) {
    }

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

    /// Command line.
    ///
    /// @return the command line
    ///
    @Override
    public CommandLine commandLine() {
        return commandLine;
    }

    /// Property.
    ///
    /// @param name the name
    /// @param defaultValue the default value
    /// @return the string
    ///
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

    /// Return a carrier with this context available from [#context].
    ///
    /// @return the carrier
    ///
    public ScopedValue.Carrier inScope() {
        return ScopedValue.where(scopedBuildContext, this);
    }

    /// Return a carrier with this context available from [#context] and
    /// the provider invocation allowed flag set.
    ///
    /// @param carrier the carrier
    /// @return the augmented carrier
    ///
    /* default */ ScopedValue.Carrier inScopeForProviderCall() {
        return inScope()
            .where(providerInvocationAllowed, new AtomicBoolean(true));
    }

    /* default */ SplitConsole console() {
        return console;
    }

    /// Status line.
    ///
    /// @return the status line
    ///
    @Override
    public StatusLine statusLine() {
        return FutureStream.statusLine.orElse(SplitConsole.nullStatusLine());
    }

    /// Out.
    ///
    /// @return the prints the stream
    ///
    @Override
    public PrintStream out() {
        return console().out();
    }

    /// Error.
    ///
    /// @return the prints the stream
    ///
    @Override
    public PrintStream error() {
        return console().err();
    }

    /// Resources.
    ///
    /// @param <T> the generic type
    /// @param provider the provider
    /// @param request the request
    /// @return the stream
    ///
    @Override
    public <T extends Resource> Stream<T> resources(ResourceProvider provider,
            ResourceRequest<T> request) {
        // Normalize request, non-project providers don't get intends
        var invocation = new ProviderInvocation<>(provider,
            provider instanceof Project || request.uses().isEmpty() ? request
                : request.using(EnumSet.noneOf(Intent.class)));
        return inScopeForProviderCall()
            .call(() -> inResourcesContext(invocation));
    }

    @SuppressWarnings({ "PMD.AvoidSynchronizedStatement",
        "PMD.AvoidCatchingGenericException" })
    private <T extends Resource> Stream<T> inResourcesContext(
            ProviderInvocation<T> invocation) {
        if (invocation.provider() instanceof Project) {
            // As a project's provide only delegates to other providers
            // it is inefficient to invoke it asynchronously. Nevertheless,
            // SPI must be invoked lazily.
            var snapshot = ScopedValueContext.snapshot();
            return Stream.generate(() -> {
                try {
                    return snapshot.where(providerInvocationAllowed,
                        new AtomicBoolean(true))
                        .call(() -> invokeSpi(invocation));
                } catch (Exception e) {
                    throw new BuildException().cause(e);
                }
            }).limit(1).flatMap(Collection::stream);
        }
        if (!invocation.request().type().equals(CleanlinessType)) {
            return cache.computeIfAbsent(invocation,
                k -> new FutureStream<T>(k)).stream();
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
        var result = invokeSpi(invocation).stream();
        // Purge cached results from provider
        cache.purge(invocation.provider());
        return result;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private <T extends Resource> Collection<T>
            invokeSpi(ProviderInvocation<T> invocation) {
        return ScopedValue
            .where(requestChainEnd, new RequestChainLink(
                requestChainEnd.isBound() ? requestChainEnd.get()
                    : new RequestChainLink(null, ProviderInvocation.LAUNCH),
                invocation))
            .call(() -> {
                logger.atFinest().log("Request chain: %s",
                    lazy(() -> requestChain()
                        .stream().map(ProviderInvocation::toString)
                        .collect(Collectors.joining(" ≪ "))));
                var prev = requestChainEnd.get().previous;
                while (prev != null) {
                    if (invocation.equals(prev.invocation())) {
                        throw new BuildException().message("Request loop: %s",
                            requestChain().stream()
                                .map(ProviderInvocation::toString)
                                .collect(Collectors.joining(" ≪ ")));
                    }
                    prev = prev.previous;
                }
                return ((AbstractProvider) invocation.provider()).toSpi()
                    .provide(invocation.request());
            });
    }

    /* default */ List<ProviderInvocation<?>> requestChain() {
        var cur = requestChainEnd.isBound() ? requestChainEnd.get() : null;
        List<ProviderInvocation<?>> result = new LinkedList<>();
        while (cur != null) {
            result.add(cur.invocation);
            cur = cur.previous;
        }
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

    /* default */ Future<AbstractRootProject> buildProject() {
        return buildProject;
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
                    scopedBuildContext.get().buildProject.complete(result);
                    return result;
                });
        } catch (SecurityException | NegativeArraySizeException
                | IllegalArgumentException | ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
