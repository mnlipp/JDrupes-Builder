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

/// Models a [ResourceProvider] that generates new [Resource]s (artifacts)
/// and makes them available to a [Project].
///
/// In general, generators follow one of the following patterns:
///
///  1. They generate resources from arbitrary inputs. In this case,
///     methods for adding inputs should be named as appropriate for
///     the generator and the type of input.
///
///  2. They generate resources from explicitly specified resources.
///     In this case, methods for adding inputs should be named
///     `add(Type... values)` for enumerated values and
///     `addXxx(Stream<Type> values)` for streams. (We cannot defines
///     a generic `add(Stream<T>)` method due to type erasure.)
///
///  3. They generate resources from resources actively obtained from
///     [ResourceProvider]s. In this case, methods for adding providers
///     should be named `from(...)`. This can be enforced by implementing
///     [ResourceRetriever].
///
/// All generators must handle requests for [Cleanliness].
///
public interface Generator extends ResourceProvider {

    /// Returns the generator's name.
    ///
    /// @return the string
    ///
    String name();

    /// Returns the project that this generator belongs to.
    ///
    /// @return the project
    ///
    Project project();

}