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

import java.util.stream.Stream;

/// A provider of a resource. This interface is intended to be implemented
/// by providers. It is not intended to be invoked directly. Rather, it 
/// must be invoked via [BuildContext#resources].
/// 
/// The interface also serves as a factory for creating resource
/// requests to be passed to the providers.
///
public interface ResourceProvider {

    /// Provide the requested resources.
    /// 
    /// This method is never invoked concurrently for the same request
    /// when invoked through [BuildContext#resources]. It may, however,
    /// be invoked concurrently for different requests. Providers that
    /// evaluate all possibly provided resources anyway and return only
    /// a subset for some kinds of request should therefore invoke
    /// themselves (through [BuildContext#resources]) with a request for
    /// all resources and filter the (automatically cached) result.
    ///
    /// @param <T> the type of the requested (and provided) resource
    /// @param requested the requested resources
    /// @return the provided resource(s) as stream
    ///
    <T extends Resource> Stream<T> provide(ResourceRequest<T> requested);

    /// Returns resources provided by this provider. Short for
    /// `context().resources(this, request)`.
    ///
    /// @param <T> the generic type
    /// @param requested the request
    /// @return the stream
    ///
    default <T extends Resource> Stream<T>
            resources(ResourceRequest<T> requested) {
        return context().resources(this, requested);
    }

    /// Returns the build context.
    ///
    /// @return the builder configuration
    ///
    BuildContext context();

    /// Create a new request for the given resource.
    ///
    /// @param <T> the generic type
    /// @param type the type
    /// @return the resource request
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    <T extends Resource> ResourceRequest<T> of(ResourceType<? extends T> type);

    /// Creates a request for a resource of the given type in a
    /// container of type [Resources]. The recommended usage pattern
    /// is to import this method statically.
    ///
    /// @param <T> the generic type
    /// @param requested the requested
    /// @return the resource request
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    default <T extends Resource> ResourceRequest<T> of(Class<T> requested) {
        return of(new ResourceType<>(requested, null));
    }
}
