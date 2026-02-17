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

/// An AwaitableCounter.
///
@SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
public class AwaitableCounter {
    private int count;

    /// Initializes a new awaitable counter.
    ///
    public AwaitableCounter() {
        // Make javadoc happy
    }

    /// Increment.
    ///
    /// @return the awaitable counter
    ///
    public synchronized AwaitableCounter increment() {
        count++;
        return this;
    }

    /// Decrement.
    ///
    /// @return the awaitable counter
    ///
    public synchronized AwaitableCounter decrement() {
        count--;
        if (count == 0) {
            notifyAll();
        }
        return this;
    }

    /// Create a count.
    ///
    /// @return the count
    ///
    public Count count() {
        return new Count();
    }

    /// Await zero.
    ///
    /// @param wanted the wanted
    /// @return the awaitable counter
    /// @throws InterruptedException the interrupted exception
    ///
    public synchronized AwaitableCounter await(int wanted)
            throws InterruptedException {
        while (count != wanted) {
            wait();
        }
        return this;
    }

    /// Increment/decrement with an AutoCloseable.
    ///
    public class Count implements AutoCloseable {

        /// Initializes a new count.
        ///
        @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
        public Count() {
            increment();
        }

        /// Close.
        ///
        @Override
        public void close() {
            decrement();
        }
    }
}