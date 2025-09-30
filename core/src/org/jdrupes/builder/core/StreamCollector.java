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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/// A [StreamCollector] allows the user to combine several [Stream]s
/// into one. The collected streams are terminated when
/// [#stream] is called. If the collector is cached, [#stream] can
/// be invoked several times and each invocation returns a new Stream
/// with the collected content.
///
/// Note that cached collectors are implemented in a straightforward
/// manner by putting all contents in a list and then returning a
/// stream for the list.
///
/// @param <T> the generic type
///
public class StreamCollector<T> {

    private List<Stream<T>> sources = new ArrayList<>();
    private List<T> cache;
    private final boolean cached;

    /// Instantiates a new collector.
    ///
    /// @param cached determines if contents is cached
    ///
    public StreamCollector(boolean cached) {
        this.cached = cached;
    }

    /// Use all given streams as sources.
    ///
    /// @param sources the sources
    /// @return the stream collector
    ///
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final StreamCollector<T> add(Stream<? extends T>... sources) {
        if (this.sources == null) {
            throw new IllegalStateException(
                "Cannot add sources after stream() has been called.");
        }
        this.sources.addAll((List<Stream<T>>) (Object) Arrays.asList(sources));
        return this;
    }

    /// Convenience method for adding a enumerated items.
    ///
    /// @param items the item
    /// @return the stream collector
    ///
    @SafeVarargs
    public final StreamCollector<T> add(T... items) {
        if (sources == null) {
            throw new IllegalStateException(
                "Cannot add sources after stream() has been called.");
        }
        sources.add(Arrays.stream(items));
        return this;
    }

    /// Provide the contents from the stream(s).
    ///
    /// @return the stream<? extends t>
    ///
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    public Stream<T> stream() {
        synchronized (this) {
            if (cache != null) {
                return cache.stream();
            }
            Stream<T> result;
            if (cached) {
                cache = sources.stream().flatMap(s -> s).toList();
                result = cache.stream();
            } else {
                result = sources.stream().flatMap(s -> s);
            }
            sources = null;
            return result;
        }
    }

    /// Create a cached collector.
    ///
    /// @param <T> the generic type
    /// @return the cached stream
    ///
    public static <T> StreamCollector<T> cached() {
        return new StreamCollector<>(true);
    }

    /// Create a cached collector initially containing a single source stream.
    ///
    /// @param <T> the generic type
    /// @param source the source
    /// @return the cached stream
    ///
    public static <T> StreamCollector<T> cached(Stream<T> source) {
        var result = new StreamCollector<T>(true);
        result.add(source);
        return result;
    }

    /// Create an un-cached collector.
    ///
    /// @param <T> the generic type
    /// @return the cached stream
    ///
    public static <T> StreamCollector<T> uncached() {
        return new StreamCollector<>(false);
    }

    /// Create an un-cached collector initially containing a single
    /// source stream.
    ///
    /// @param <T> the generic type
    /// @param source the source
    /// @return the cached stream
    ///
    public static <T> StreamCollector<T> uncached(Stream<T> source) {
        var result = new StreamCollector<T>(false);
        result.add(source);
        return result;
    }
}
