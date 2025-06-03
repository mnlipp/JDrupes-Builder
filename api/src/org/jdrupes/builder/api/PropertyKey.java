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

/// The Interface defines the type used as key for a [Project]'s properties.
/// Implementations of this interface should extends [Enum]. See
/// [Project.Properties] for an example.
///
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface PropertyKey {

    /// The property's name.
    ///
    /// @return the string
    ///
    String name();

    /// The property's type.
    ///
    /// @return the class
    ///
    default Class<?> type() {
        return defaultValue().getClass();
    }

    /// The property's default value. This value must not be `null`.
    ///
    /// @return the object
    ///
    Object defaultValue();

}
