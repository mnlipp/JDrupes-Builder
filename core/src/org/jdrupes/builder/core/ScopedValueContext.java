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

import java.lang.ScopedValue.CallableOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

/// Supports using scoped values in another context.
/// 
/// Scoped values are bound in a thread context. However, their binding
/// is no longer available when an action is executed in another thread
/// or as a callback. 
///
public final class ScopedValueContext {

    private static List<ScopedValue<?>> registry = new CopyOnWriteArrayList<>();
    private static final ScopedValue<Boolean> DUMMY = ScopedValue.newInstance();

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
        private ScopedValue.Carrier carrierList() {
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

        /// Returns the carriers for the values in the snapshot.
        ///
        /// @return the scoped value. carrier
        ///
        public ScopedValue.Carrier carriers() {
            return Optional.ofNullable(carrierList())
                .orElseGet(() -> ScopedValue.where(DUMMY, Boolean.TRUE));
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
            return Optional.ofNullable(carrierList())
                .map(c -> c.where(key, value))
                .orElseGet(() -> ScopedValue.where(key, value));
        }

        /// Invokes the appender for adding scoped values to the snapshot
        /// and returns the result.
        ///
        /// @param <T> the generic type
        /// @param appender the appender
        /// @return the scoped value. carrier
        ///
        public <T> ScopedValue.Carrier where(
                Function<ScopedValue.Carrier, ScopedValue.Carrier> appender) {
            return appender.apply(carriers());
        }

        /// Short for `carriers().call(op)`.
        ///
        /// @param <R> the generic type
        /// @param <X> the generic type
        /// @param op the op
        /// @return the r
        /// @throws X the x
        ///
        @SuppressWarnings("PMD.ShortVariable")
        public <R, X extends Throwable> R call(CallableOp<? extends R, X> op)
                throws X {
            var carriers = carrierList();
            if (carriers == null) {
                return op.call();
            }
            return carriers.call(op);
        }

        /// Short for `carriers().run(task)`.
        ///
        /// @param task the task
        ///
        public void run(Runnable task) {
            var carriers = carrierList();
            if (carriers == null) {
                task.run();
            } else {
                carriers.run(task);
            }
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
            return snapshot.call(task::call);
        });
    }
}
