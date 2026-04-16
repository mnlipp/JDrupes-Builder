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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/// Supports inheriting scoped values in a task submitted to an
/// executor service.
///
public final class ScopedValueInheritance {

    private static List<ScopedValue<?>> registry = new CopyOnWriteArrayList<>();

    private ScopedValueInheritance() {
        // Make javadoc happy
    }

    /// Adds the value to the registry.
    ///
    /// @param value the value
    ///
    public static void add(ScopedValue<?> value) {
        registry.add(value);
    }

    /// Executes the task with the registered scoped values inherited
    /// from the current thread.
    ///
    /// @param <T> the generic type
    /// @param executor the executor
    /// @param task the task
    /// @return the future
    ///
    @SuppressWarnings("unchecked")
    public static <T> Future<T> submitTo(ExecutorService executor,
            Callable<T> task) {
        // Capture values
        final var scoped = new ArrayList<>(registry);
        final var values = new ArrayList<>(scoped.size());
        for (var scopedVar : scoped) {
            values.add(scopedVar.isBound() ? scopedVar.get() : null);
        }

        // Restore values in submitted task
        return executor.submit(() -> {
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
            if (carriers == null) {
                return task.call();
            }
            return carriers.call(task::call);
        });
    }
}
