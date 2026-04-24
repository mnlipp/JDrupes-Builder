/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/// A stream of elements whose backing collection is obtained lazily from
/// a supplier when the stream is first consumed.
///
/// @param <T> the element type
///
public final class LazyCollectionStream<T>
        extends Spliterators.AbstractSpliterator<T> {

    private final Supplier<Collection<T>> supplier;
    private Iterator<T> theIterator;

    private LazyCollectionStream(Supplier<Collection<T>> supplier) {
        super(Long.MAX_VALUE, IMMUTABLE);
        this.supplier = supplier;
    }

    private Iterator<T> iterator() {
        if (theIterator == null) {
            theIterator = supplier.get().iterator();
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

    /// Creates a stream backed by a collection supplied lazily by the
    /// given supplier. The supplier is invoked only when the stream is
    /// first consumed.
    ///
    /// @param <T> the element type
    /// @param supplier supplies the collection
    /// @return the stream
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    public static <T> Stream<T> of(Supplier<Collection<T>> supplier) {
        return StreamSupport.stream(
            new LazyCollectionStream<>(supplier), false);
    }
}
