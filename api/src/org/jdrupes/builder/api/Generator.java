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