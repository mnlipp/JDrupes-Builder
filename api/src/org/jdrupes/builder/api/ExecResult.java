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

package org.jdrupes.builder.api;

import java.util.stream.Stream;

/// Provides the results from executing a process. The type of resource
/// related to the execution depends on the provider. It is recommended
/// to this as a base interface for defining execution results with
/// the resource type bound to a specific type.
///
/// @param <T> the resource type
///
public interface ExecResult<T extends Resource> extends Resource, FaultAware {

    /// The exit value.
    ///
    /// @return the exit value
    ///
    int exitValue();

    /// Returns the stream of resources produced by the execution.
    ///
    /// @return the stream
    ///
    default Stream<T> resources() {
        return Stream.empty();
    }

    /// Creates a new execution result from the given values.
    ///
    /// @param provider the provider
    /// @param name the name
    /// @param exitValue the exit value
    /// @return the exec result
    ///
    static ExecResult<?> from(ResourceProvider provider, String name,
            int exitValue) {
        return ResourceFactory.create(
            new ResourceType<>() {}, provider, name, exitValue);
    }

    /// Creates a new execution result from the given values.
    ///
    /// @param <T> the generic type
    /// @param provider the provider
    /// @param name the name
    /// @param exitValue the exit value
    /// @param resources the resources
    /// @return the exec result
    ///
    static <T extends Resource> ExecResult<T> from(ResourceProvider provider,
            String name, int exitValue, Stream<T> resources) {
        return ResourceFactory.create(
            new ResourceType<>() {}, provider, name, exitValue, resources);
    }
}
