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
/// must be invoked via [BuildContext#get].
///
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ResourceProvider {

    /// Provide the requested resources.
    /// 
    /// This method is never invoked concurrently for the same request
    /// when invoked through [Project#get]. It may, however, be invoked
    /// concurrently for different requests. Providers that evaluate all
    /// possibly provided resources anyway and return only a subset for
    /// some kinds of request should therefore invoke themselves (through
    /// [Project#get]) with a request for all resources and filter the
    /// (automatically cached) result.
    ///
    /// @param <T> the type of the requested (and provided) resource
    /// @param requested the requested resources
    /// @return the provided resource(s) as stream
    ///
    <T extends Resource> Stream<T> provide(ResourceRequest<T> requested);
}
