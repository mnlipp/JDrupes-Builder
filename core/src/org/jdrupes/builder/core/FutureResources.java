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

import java.time.Instant;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Provider;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;

/// The Class FutureResources.
///
/// @param <T> the contained resource type
///
public class FutureResources<R extends Resource> implements Resources<R> {

    private final Future<Resources<R>> source;

    /// Instantiates a new future resources.
    ///
    /// @param executor the executor
    /// @param provider the provider
    /// @param requested the requested
    ///
    public FutureResources(ExecutorService executor, Provider<R> provider,
            Resource requested) {
        source = executor.submit(() -> provider.provide(requested));
    }

    @Override
    public Instant asOf() {
        try {
            return source.get().asOf();
        } catch (InterruptedException | ExecutionException e) {
            throw new BuildException(e);
        }
    }

    @Override
    public Resources<R> add(R resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resources<R> addAll(Resources<R> resources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<R> stream() {
        return StreamSupport
            .stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, 0) {

                @Override
                public void forEachRemaining(Consumer<? super R> action) {
                    try {
                        source.get().stream().forEach(action);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new BuildException(e);
                    }
                }

                @Override
                public boolean tryAdvance(Consumer<? super R> action) {
                    // Not needed when forEachRemaining is implemented.
                    return false;
                }
            }, false);
    }

}
