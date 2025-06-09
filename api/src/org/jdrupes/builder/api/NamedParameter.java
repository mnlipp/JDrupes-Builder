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

import java.util.function.Supplier;

/// Defines a named parameter. Java doesn't have named parameters, but
/// this comes pretty close. Define the named parameters that you need
/// as static members of your class:
///
///     public static NamedParameter<String> name(String name) {
///         return new NamedParameter<>("name", name);
///     }
/// 
/// Defines the method that is to be invoked with named parameters as
///
///     public void method(NamedParameter<?>... params) {
///         var name = NamedParameter.<String> get(params, "name", null);
///     }
///
/// And invoke it with:
///
///     method(name("test"));
///
/// Of course, this requires a static import for `name`. If you have several
/// classes using the pattern, you'll have to use `className.paramName(...)`
/// to provide the value for the parameter which is, admittedly less elegant.
///
/// A possible workaround is to define a hierarchy of classes with
/// [NamedParameter] as base class and put commonly used names in the base
/// classes. 
///
/// @param <T> the generic type
/// @param name the name
/// @param value the value
///
public record NamedParameter<T>(String name, T value) {

    /// Looks up the named parameter with the given `name` in array
    /// of [NamedParameter]s. If it isn't found, return the result
    /// from invoking the supplier (or `null`).
    ///
    /// @param <T> the generic type
    /// @param parameters the parameters
    /// @param name the name
    /// @param fallback the fallback or `null`
    /// @return the t
    ///
    @SuppressWarnings("unchecked")
    public static <T> T get(NamedParameter<?>[] parameters, String name,
            Supplier<T> fallback) {
        for (var param : parameters) {
            if (param.name.equals(name)) {
                return (T) param.value;
            }
        }
        if (fallback != null) {
            return fallback.get();
        }
        return null;
    }
}