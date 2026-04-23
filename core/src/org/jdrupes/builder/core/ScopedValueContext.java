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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/// Supports using scoped values in another context.
/// 
/// Scoped values are bound in a thread context. However, their binding
/// is no longer available when an action is executed in another thread
/// or as a callback. 
///
public final class ScopedValueContext {

    private static List<ScopedValue<?>> registry = new CopyOnWriteArrayList<>();

    /// A snapshot of the the values of the registered scoped value instances.
    ///
    public static final class Snapshot {
        private final List<ScopedValue<?>> scoped = new LinkedList<>(registry);
        private final List<Object> values = new ArrayList<>(scoped.size());

        private Snapshot() {
            for (var iter = scoped.iterator(); iter.hasNext();) {
                var scopedVar = iter.next();
                if (scopedVar.isBound()) {
                    values.add(scopedVar.get());
                } else {
                    iter.remove();
                }
            }
        }

        @SuppressWarnings("unchecked")
        private ScopedValue.Carrier carriers() {
            var scopedIterator = scoped.iterator();
            var valuesIterator = values.iterator();
            ScopedValue.Carrier carriers = null;
            if (scopedIterator.hasNext()) {
                carriers = ScopedValue.where(
                    (ScopedValue<Object>) scopedIterator.next(),
                    valuesIterator.next());
            }
            while (scopedIterator.hasNext()) {
                carriers = carriers.where(
                    (ScopedValue<Object>) scopedIterator.next(),
                    valuesIterator.next());
            }
            return carriers;
        }

        /// Appends the scoped value and value to the carriers representing
        /// the snapshot and returns the result.
        ///
        /// @param <T> the generic type
        /// @param key the key
        /// @param value the value
        /// @return the scoped value. carrier
        ///
        public <T> ScopedValue.Carrier where(ScopedValue<T> key, T value) {
            return Optional.ofNullable(carriers()).map(c -> c.where(key, value))
                .orElseGet(() -> ScopedValue.where(key, value));

        }

        /// Get the value from the given supplier after restoring the
        /// values of the registered scoped value instances.
        ///
        /// @param <T> the generic type
        /// @param action the action
        /// @return the result
        ///
        public <T> T withGet(Supplier<T> action) {
            ScopedValue.Carrier carriers = carriers();
            if (carriers == null) {
                return action.get();
            }
            return carriers.call(action::get);
        }

        /// Execute the given task after restoring the values of the
        /// registered scoped value instances.
        ///
        /// @param <T> the generic type
        /// @param task the action
        /// @return the result
        /// @throws Exception forwarded exception from task
        ///
        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        public <T> T withCall(Callable<T> task) throws Exception {
            ScopedValue.Carrier carriers = carriers();
            if (carriers == null) {
                return task.call();
            }
            return carriers.call(task::call);
        }
    }

    private ScopedValueContext() {
        // Make javadoc happy
    }

    /// Adds the value to the registry.
    ///
    /// @param values the values
    ///
    public static void add(ScopedValue<?>... values) {
        Arrays.asList(values).forEach(registry::add);
    }

    /// Creates a new snapshot.
    ///
    /// @return the snapshot
    ///
    public static Snapshot snapshot() {
        return new Snapshot();
    }

    /// Executes the task with the registered scoped values inherited
    /// from the current thread.
    ///
    /// @param <T> the generic type
    /// @param executor the executor
    /// @param task the task
    /// @return the future
    ///
    public static <T> Future<T> submitTo(ExecutorService executor,
            Callable<T> task) {
        // Capture values
        Snapshot snapshot = snapshot();
        return executor.submit(() -> {
            return snapshot.withCall(task);
        });
    }
}
