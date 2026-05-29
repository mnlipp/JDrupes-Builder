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

import java.lang.reflect.Type;

/// Instances of this class define keys for a [Project]'s properties.
/// See [CoreProperties] for examples.
///
/// @param <T> the value's type
///
public class PropertyKey<T> {

    private final Type type;
    private final T defaultValue;

    /// Initializes a PropertyKey with the given default value. The
    /// type information is obtained from the value.
    ///
    /// @param defaultValue the default value
    ///
    @SuppressWarnings("unchecked")
    public PropertyKey(T defaultValue) {
        this.defaultValue = defaultValue;
        type = (Class<T>) defaultValue.getClass();
    }

    /// Initializes a PropertyKey with the given type information.
    /// The default value is set to `null`.
    ///
    /// @param type the type
    ///
    public PropertyKey(Type type) {
        this.type = type;
        defaultValue = null;
    }

    /// Returns the property's type.
    ///
    /// @return the type
    ///
    public Type type() {
        return type;
    }

    /// Returns the property's default value.
    ///
    /// @return the default value
    ///
    public T defaultValue() {
        return defaultValue;
    }

}
