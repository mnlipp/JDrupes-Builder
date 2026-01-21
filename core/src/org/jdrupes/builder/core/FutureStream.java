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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;

/// Evaluate the stream from a provider asynchronously.
///
/// @param <T> the provided resource type
///
public class FutureStream<T extends Resource> {

    /// The logger.
    protected final Logger log = Logger.getLogger(FutureStream.class.getName());
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final ScopedValue<AtomicBoolean> providerInvocationAllowed
        = ScopedValue.newInstance();
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final ScopedValue<FutureStream<?>> caller
        = ScopedValue.newInstance();
    private final FutureStream<?> initiallyCalledBy;
    private final FutureStreamCache.Key<?> holding;
    private final Future<List<T>> values;

    /// Instantiates a new future resources.
    ///
    /// @param executor the executor
    /// @param provider the provider
    /// @param request the requested
    ///
    public FutureStream(ExecutorService executor, ResourceProvider provider,
            ResourceRequest<T> request) {
        initiallyCalledBy = caller.isBound() ? caller.get() : null;
        holding = new FutureStreamCache.Key<>(provider, request);
        log.finer(() -> "Evaluating " + holding.request() + " → "
            + holding.provider()
            + (initiallyCalledBy != null ? " requested by " + initiallyCalledBy
                : ""));
        log.finest(() -> "Call chain: " + callChain());
        values = executor.submit(() -> {
            return ScopedValue
                .where(providerInvocationAllowed, new AtomicBoolean(true))
                .where(caller, this)
                .call(() -> ((AbstractProvider) provider).toSpi()
                    .provide(request).toList());
        });
    }

    private List<FutureStream<?>> callChain() {
        List<FutureStream<?>> result = new LinkedList<>();
        FutureStream<?> cur = this;
        do {
            result.add(0, cur);
            cur = cur.initiallyCalledBy;
        } while (cur != null);
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

    /// Returns the lazily evaluated stream of resources.
    ///
    /// @return the stream
    ///
    public Stream<T> stream() {
        return StreamSupport
            .stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, 0) {

                private Iterator<T> theIterator;

                private Iterator<T> iterator() {
                    if (theIterator == null) {
                        try {
                            theIterator = values.get().iterator();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new BuildException(e);
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
        return "FutureStream [" + holding.request() + " → "
            + holding.provider() + "]";
    }

}
