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
import static com.google.common.flogger.LazyArgs.*;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ConfigurationException;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.StatusLine;

/// Evaluate the stream from a provider asynchronously.
///
/// @param <T> the provided resource type
///
public class FutureStream<T extends Resource> {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final AtomicInteger futureCount = new AtomicInteger(0);
    private final DefaultBuildContext context;
    /* default */@SuppressWarnings("PMD.FieldNamingConventions")
    static final ScopedValue<StatusLine> statusLine
        = ScopedValue.newInstance(); // <T>
    private final ProviderInvocation<?> requestedBy;
    private final ProviderInvocation<?> invocation;
    private final Future<List<T>> values;
    private final int id = futureCount.getAndIncrement();

    static {
        ScopedValueInheritance.add(statusLine);
    }

    /// Instantiates a new future stream of resources.
    ///
    /// @param context the context
    /// @param invocation the invocation
    ///
    public FutureStream(
            DefaultBuildContext context, ProviderInvocation<T> invocation) {
        this.context = context;
        this.invocation = invocation;
        requestedBy = context.callChainEnd().previous().invocation();
        logger.atFiner().log("%s starting (≪ %s)", this,
            lazy(() -> requestedBy != null ? requestedBy : "(unknown)"));
        logger.atFinest().log("Call chain: %s", lazy(() -> context.callChain()
            .stream().map(ProviderInvocation::toString)
            .collect(Collectors.joining(" ≪ "))));
        final var provider = invocation.provider();
        final var request = invocation.request();
        values = ScopedValueInheritance.submitTo(context.executor(), () -> {
            var origThreadName = Thread.currentThread().getName();
            try (var _ = context.executingFutureStreams().acquire();
                    var statusLine = context.console().statusLine()) {
                Thread.currentThread().setName(
                    provider + " ← " + request.type());
                // Wait for the build-project to be fully constructed
                context.buildProject().get();
                statusLine.update(provider + " evaluating " + request);
                return context.inScopeForProviderCall()
                    .where(FutureStream.statusLine, statusLine)
                    .call(() -> ((AbstractProvider) provider).toSpi()
                        .provide(request).toList());
            } finally {
                logger.atFiner().log("%s terminated", this);
                Thread.currentThread().setName(origThreadName);
            }
        });
    }

    /// Returns the lazily evaluated stream of resources.
    ///
    /// @return the stream
    ///
    public Stream<T> stream() {
        return StreamSupport.stream(
            new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, 0) {

                private Iterator<T> theIterator;

                private Iterator<T> iterator() {
                    if (theIterator == null) {
                        if (!context.buildProject().isDone()) {
                            throw new ConfigurationException()
                                .from(invocation.provider())
                                .message("Attempt to terminate resource stream"
                                    + " while constructing the build project.");
                        }
                        try {
                            theIterator = values.get().iterator();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new BuildException()
                                .from(invocation.provider()).cause(e);
                        }
                    }
                    return theIterator;
                }

                @Override
                public void forEachRemaining(Consumer<? super T> action) {
                    iterator().forEachRemaining(action);
                }

                @Override
                public boolean tryAdvance(Consumer<? super T> action) {
                    if (!iterator().hasNext()) {
                        return false;
                    }
                    action.accept(iterator().next());
                    return true;
                }

            }, false);
    }

    @Override
    public String toString() {
        return "FutureStream#" + id + " [" + invocation.provider() + " ← "
            + invocation.request() + "]";
    }

}
