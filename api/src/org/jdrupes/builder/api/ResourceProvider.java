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
/// by providers. Requests should always be made against the [Build]
/// (see [Build#provide]).
///
/// @param <T> the provided type of [Resource]
///
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ResourceProvider<T extends Resource> {

    /// Provide the requested resource. 
    /// 
    /// This method is never invoked concurrently for the same requested
    /// resource. It may, however, be invoked concurrently for different
    /// requested resources. Providers that evaluate all resources anyway
    /// should invoke themselves through [Build#provide] with a request
    /// for all resources to avoid concurrency and only filter the result
    /// in the original thread.
    ///
    /// @param <R> the type of the requested (and provided) resources
    /// @param requested the requested resource
    /// @return the provided resource(s) as stream
    ///
    <R extends Resource> Stream<R> provide(ResourceRequest<R> requested);
}
