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
import java.util.LinkedList;
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
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
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
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final ScopedValue<FutureStream<?>> caller
        = ScopedValue.newInstance();
    /* default */@SuppressWarnings("PMD.FieldNamingConventions")
    static final ScopedValue<StatusLine> statusLine
        = ScopedValue.newInstance(); // <T>
    private final FutureStream<?> initiallyCalledBy;
    private final FutureStreamCache.Key<?> holding;
    private final Future<List<T>> values;
    private final int id = futureCount.getAndIncrement();

    /// Instantiates a new future stream of resources.
    ///
    /// @param context the context
    /// @param provider the provider
    /// @param request the requested
    ///
    public FutureStream(DefaultBuildContext context,
            ResourceProvider provider, ResourceRequest<T> request) {
        this.context = context;
        holding = new FutureStreamCache.Key<>(provider, request);
        initiallyCalledBy = caller.isBound() ? caller.get() : null;
        logger.atFiner().log("%s starting (≪ %s)", this,
            lazy(() -> initiallyCalledBy != null
                ? "FutureStream#" + initiallyCalledBy.id
                : "(unknown)"));
        logger.atFinest().log("Call chain: %s", lazy(() -> callChain()
            .stream().map(FutureStream::toString)
            .collect(Collectors.joining(" ≪ "))));
        values = context.executor().submit(() -> {
            var origThreadName = Thread.currentThread().getName();
            try (var _ = context.executingFutureStreams().acquire();
                    var statusLine = context.console().statusLine()) {
                Thread.currentThread().setName(
                    provider + " ← " + request.type());
                // Wait for the build-project to be fully constructed
                context.buildProject().get();
                statusLine.update(provider + " evaluating " + request);
                return context.inScopeForProviderCall().where(caller, this)
                    .where(FutureStream.statusLine, statusLine).call(
                        () -> ((AbstractProvider) provider).toSpi()
                            .provide(request).toList());
            } finally {
                logger.atFiner().log("%s terminated", this);
                Thread.currentThread().setName(origThreadName);
            }
        });
    }

    private List<FutureStream<?>> callChain() {
        List<FutureStream<?>> result = new LinkedList<>();
        FutureStream<?> cur = this;
        do {
            result.add(cur);
            cur = cur.initiallyCalledBy;
        } while (cur != null);
        return result;
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
                                .from(holding.provider())
                                .message("Attempt to terminate resource stream"
                                    + " while constructing the build project.");
                        }
                        try {

                            theIterator = values.get().iterator();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new BuildException().from(holding.provider())
                                .cause(e);
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
        return "FutureStream#" + id + " [" + holding.provider() + " ← "
            + holding.request() + "]";
    }

}
