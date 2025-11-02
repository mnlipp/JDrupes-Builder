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

package org.jdrupes.builder.api;

import java.util.Objects;
import java.util.Optional;

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
public class ResourceRequest<T extends Resource> {

    private final ResourceType<? extends Resources<T>> type;

    /// Instantiates a new resource request without any restriction.
    ///
    /// @param type the requested type
    ///
    public ResourceRequest(ResourceType<? extends Resources<T>> type) {
        this.type = type;
    }

    /// Creates a request for a resource of the given type, in the
    /// given container type. The recommended usage pattern is
    /// to import this method statically.
    ///
    /// @param <C> the generic type
    /// @param <T> the generic type
    /// @param container the container
    /// @param requested the requested
    /// @return the resource request
    ///
    public static <C extends Resources<T>, T extends Resource>
            ResourceRequest<T>
            requestFor(Class<C> container, Class<T> requested) {
        return new ResourceRequest<>(new ResourceType<>(container,
            new ResourceType<>(requested, null)));
    }

    /// Creates a request for a resource of the given type in a
    /// container of type [Resources]. The recommended usage pattern
    /// is to import this method statically.
    ///
    /// @param <T> the generic type
    /// @param requested the requested
    /// @return the resource request
    ///
    public static <T extends Resource>
            ResourceRequest<T> requestFor(Class<T> requested) {
        return new ResourceRequest<>(new ResourceType<>(Resources.class,
            new ResourceType<>(requested, null)));
    }

    /// Slightly briefer alternative to invoking the constructor.
    ///
    /// @param <T> the generic type
    /// @param type the type
    /// @return the resource request
    ///
    public static <T extends Resource> ResourceRequest<T>
            requestFor(ResourceType<? extends Resources<T>> type) {
        return new ResourceRequest<>(type);
    }

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
    public <R extends Resources<T>> ResourceRequest<T> widened(
            @SuppressWarnings("rawtypes") Class<? extends Resources> type) {
        return new ResourceRequest<>(type().widened(type));
    }

    /// Return the requested type.
    ///
    /// @return the resource type
    ///
    public ResourceType<? extends Resources<T>> type() {
        return type;
    }

    /// Checks if this request accepts a resource of the given type.
    /// Short for `type().isAssignableFrom(other)`.
    ///
    /// @param other the other
    /// @return true, if successful
    ///
    public boolean wants(ResourceType<?> other) {
        return type().isAssignableFrom(other);
    }

    /// Checks if the requested resource type includes the given type.
    ///
    /// @param type the type to check
    /// @return true, if successful
    ///
    public boolean includes(ResourceType<?> type) {
        return Optional.ofNullable(type().containedType())
            .map(ct -> ct.isAssignableFrom(type)).orElse(false);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ResourceRequest<?> other = (ResourceRequest<?>) obj;
        return Objects.equals(type, other.type);
    }

    @Override
    public String toString() {
        return "ResourceRequest [type=" + type + "]";
    }

}
