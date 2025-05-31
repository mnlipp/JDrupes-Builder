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

/// Attributed dependency relationship between a [Project] and 
/// a [ResourceProvider].
///
/// @param provider the provider
/// @param intend the intended usage of the dependency
///
public record Dependency(ResourceProvider<?> provider, Intend intend) {

    /// Defines how resources from the dependent project are to be used.
    ///
    @SuppressWarnings("PMD.FieldNamingConventions")
    public enum Intend {
        
        /// Requests for resources are forwarded, but the results are
        /// not used. This is the default relationship between a project
        /// and its sub projects.
        Build,
        
        /// Resources from the provider are used, but not exposed
        /// i.e. they are not provided to others.
        Consume,
        
        /// Resources from the provider are used and exposed, i.e.
        /// made available to others.
        Expose,
        
        /// Resources are only required at runtime.
        Runtime
    }
}
