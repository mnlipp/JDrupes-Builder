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

    /// Provide the requested resource. Most providers will only
    /// evaluate the requested resource kind and return resources
    /// of the requested kind.
    ///
    /// The restriction imposed on the returned values by `T` isn't
    /// very strong. Therefore the following rules apply:
    ///
    /// | `requested`'s kind | Returned values' type  |
    /// |--------------------|------------------------|
    /// | `KIND_CLASSES`     | [FileTree]             |
    /// | `KIND_RESOURCES`   | [FileTree]             |
    ///
    /// @param requested the requested resource
    /// @return the provided resource(s) as stream
    ///
    Stream<T> provide(Resource requested);
}
