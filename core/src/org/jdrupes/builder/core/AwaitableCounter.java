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

/// A counter that supports waiting for a specific value. The waiting
/// thread is woken when the counter hits the target value.
///
@SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
public class AwaitableCounter {
    private int count;

    /// Initializes a new awaitable counter.
    ///
    public AwaitableCounter() {
        // Make javadoc happy
    }

    /// Increment the count's value.
    ///
    /// @return the counter
    ///
    public synchronized AwaitableCounter increment() {
        count++;
        return this;
    }

    /// Decrement the counter's value.
    ///
    /// @return the counter
    ///
    public synchronized AwaitableCounter decrement() {
        count--;
        if (count == 0) {
            notifyAll();
        }
        return this;
    }

    /// Return the counter's value.
    ///
    /// @return the value
    ///
    public int value() {
        return count;
    }

    /// Create an increment.
    ///
    /// @return the count
    ///
    public Increment acquire() {
        return new Increment();
    }

    /// Wait for the counter to reach the given value.
    ///
    /// @param wanted the wanted
    /// @return the counter
    /// @throws InterruptedException the interrupted exception
    ///
    public synchronized AwaitableCounter await(int wanted)
            throws InterruptedException {
        while (count != wanted) {
            wait();
        }
        return this;
    }

    /// Increment/decrement with an AutoCloseable. This supports
    /// reliable increment/decrement using try-with-resources.
    ///
    public class Increment implements AutoCloseable {

        /// Initializes a new Increment. Increments the counter.
        ///
        @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
        public Increment() {
            increment();
        }

        /// Decrements the counter.
        ///
        @Override
        public void close() {
            decrement();
        }
    }
}