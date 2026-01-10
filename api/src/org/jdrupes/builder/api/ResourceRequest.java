/*
 * JDrupes Builder
 * Copyright (C) 2025, 2026 Michael N. Lipp
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

/// Represents a request for [Resource]s of a specified type.
/// The specified type provides two kinds of type information:
///
/// 1. The type of the [Resource]s that are actually provided.
/// 2. The type of the "context" in which the [Resource]s are to be provided.
///
/// As an example, consider requests for a compile time and a runtime
/// classpath. In both cases, the actually provided [Resource]s are
/// of type "classpath element". However, depending on the kind of
/// classpath, a [ResourceProvider] may deliver different collections of
/// instances of "classpath elements". So instead of requesting
/// "classpath element", 
///
/// Not all requested resource types require context information. For
/// example, a request for [Cleanliness] usually refers to all resources
/// that a [Generator] has created and does not depend on a context.
/// However, in order to keep the API simple, the context is always
/// required. 
///
/// @param <T> the generic type
///
public interface ResourceRequest<T extends Resource> {

    /// Create a widened resource request by replacing the requested
    /// top-level type with the given super type, thus widening the
    /// request.
    ///
    /// @param <R> the generic type
    /// @param type the desired super type. This should actually be
    /// declared as `Class <R>`, but there is no way to specify a 
    /// parameterized type as actual parameter.
    /// @return the new resource request
    ///
    <R extends Resources<T>> ResourceRequest<T> widened(
            @SuppressWarnings("rawtypes") Class<? extends Resources> type);

    /// Return the requested type.
    ///
    /// @return the resource type
    ///
    ResourceType<? extends Resources<T>> type();

    /// Checks if this request accepts a resource of the given type.
    /// Short for `type().isAssignableFrom(other)`.
    ///
    /// @param other the other
    /// @return true, if successful
    ///
    boolean accepts(ResourceType<?> other);

    /// Checks if the requested type is a container type and if the
    /// contained type of the container type is assignable from the
    /// given type. 
    ///
    /// @param type the type to check
    /// @return true, if successful
    ///
    boolean collects(ResourceType<?> type);

}
