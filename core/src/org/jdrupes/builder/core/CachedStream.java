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

/// A cached stream allows the user of a [Stream] to access it multiple
/// times. The stream is evaluated on the first call to [#stream] and
/// then cached.
///
/// As a convenience, the class supports combining the contents of multiple
/// streams.
///
/// @param <T> the generic type
///
public class CachedStream<T> {

    private List<Stream<? extends T>> sources = new ArrayList<>();
    private List<? extends T> cache;

    /// Instantiates a new cached stream.
    ///
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public CachedStream() {
        // Make javadoc happy.
    }

    /// Use all given streams as sources.
    ///
    /// @param sources the sources
    ///
    @SafeVarargs
    public final void add(Stream<? extends T>... sources) {
        if (sources == null) {
            throw new IllegalStateException(
                "Cannot add sources after stream() has been called.");
        }
        this.sources.addAll(Arrays.asList(sources));
    }

    /// Provide the contents from the stream(s).
    ///
    /// @return the stream<? extends t>
    ///
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    public Stream<? extends T> stream() {
        synchronized (this) {
            if (cache == null) {
                cache = sources.stream().flatMap(s -> s).toList();
                sources = null;
            }
            return cache.stream();
        }
    }

    /// Create a cached stream from a single source stream.
    ///
    /// @param <T> the generic type
    /// @param source the source
    /// @return the cached stream
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    public static <T> CachedStream<T> of(Stream<T> source) {
        var result = new CachedStream<T>();
        result.add(source);
        return result;
    }
}
