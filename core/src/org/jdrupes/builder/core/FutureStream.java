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

import java.util.List;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
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

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final ThreadLocal<Boolean> providerInvocationAllowed
        = ThreadLocal.withInitial(() -> false);
    private final Future<List<T>> source;

    /// Instantiates a new future resources.
    ///
    /// @param executor the executor
    /// @param provider the provider
    /// @param requested the requested
    ///
    public FutureStream(ExecutorService executor, ResourceProvider provider,
            ResourceRequest<T> requested) {
        source = executor.submit(() -> {
            try {
                providerInvocationAllowed.set(true);
                return provider.provide(requested).toList();
            } finally {
                providerInvocationAllowed.set(false);
            }
        });
    }

    /// Checks if is provider invocation is allowed.
    ///
    /// @return true, if is provider invocation allowed
    ///
    public static boolean isProviderInvocationAllowed() {
        return providerInvocationAllowed.get();
    }

    /// Returns the lazily evaluated stream of resources.
    ///
    /// @return the stream
    ///
    public Stream<T> stream() {
        return StreamSupport
            .stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, 0) {

                @Override
                public void forEachRemaining(Consumer<? super T> action) {
                    try {
                        source.get().stream().forEach(action);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new BuildException(e);
                    }
                }

                @Override
                public boolean tryAdvance(Consumer<? super T> action) {
                    // Not needed when forEachRemaining is implemented.
                    return false;
                }
            }, false);
    }

}
