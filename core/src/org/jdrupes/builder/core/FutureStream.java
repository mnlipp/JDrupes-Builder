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
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        = ScopedValue.newInstance();
    private final ProviderInvocation<?> invocation;
    private final Future<Collection<T>> values;
    private final int id = futureCount.getAndIncrement();

    static {
        ScopedValueContext.add(statusLine);
    }

    /// Instantiates a new future stream of resources.
    ///
    /// @param invocation the invocation
    ///
    public FutureStream(ProviderInvocation<T> invocation) {
        context = DefaultBuildContext.context().get();
        this.invocation = invocation;
        final var provider = invocation.provider();
        final var request = invocation.request();
        values = ScopedValueContext.submitTo(context.executor(), () -> {
            var origThreadName = Thread.currentThread().getName();
            try (var _ = context.executingFutureStreams().acquire();
                    var statusLine = context.console().statusLine()) {
                Thread.currentThread().setName(
                    provider + " ← " + request.type());
                logger.atFiner().log(
                    "Task [%s] evaluating", Thread.currentThread().getName());
                // Wait for the build-project to be fully constructed
                context.buildProject().get();
                statusLine.update(provider + " evaluating " + request);
                return context.inScopeForProviderCall()
                    .where(FutureStream.statusLine, statusLine)
                    .call(() -> ((AbstractProvider) provider).toSpi()
                        .provide(request));
            } finally {
                logger.atFiner().log(
                    "Task [%s] terminated", Thread.currentThread().getName());
                Thread.currentThread().setName(origThreadName);
            }
        });
    }

    /// Returns the lazily evaluated stream of resources.
    ///
    /// @return the stream
    ///
    public Stream<T> stream() {
        return LazyCollectionStream.of(() -> {
            if (!context.buildProject().isDone()) {
                throw new ConfigurationException().from(invocation.provider())
                    .message("Attempt to consume resource stream"
                        + " while constructing the build project.");
            }
            try {
                logger.atFiner().log("%s awaiting result, request chain: %s",
                    this, lazy(() -> context.requestChain().stream()
                        .map(ProviderInvocation::toString)
                        .collect(Collectors.joining(" ≪ "))));
                return values.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new BuildException()
                    .from(invocation.provider()).cause(e);
            } finally {
                logger.atFiner().log("%s is done", this);
            }
        });
    }

    @Override
    public String toString() {
        return "FutureStream#" + id + " [" + invocation.provider() + " ← "
            + invocation.request().toRequestedString() + "]";
    }

}
