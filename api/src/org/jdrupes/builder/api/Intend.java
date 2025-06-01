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

import java.util.EnumSet;
import java.util.Set;

/// Attributes the relationship between a [Project] and an associated
/// [ResourceProvider].
///
@SuppressWarnings("PMD.FieldNamingConventions")
public enum Intend {
    
    /// Requests for resources are forwarded, but the results are
    /// not used. This is the default relationship between a project
    /// and its sub projects.
    Forward,
    
    /// Resources from the provider are used, but not exposed
    /// i.e. they are not provided to others.
    Consume,
    
    /// Resources from the provider are used and exposed, i.e.
    /// made available to others.
    Expose,
    
    /// The resources from the provider are provided by the project.
    /// This is the default relationship between a project and its
    /// generators.
    Provide,
    
    /// Resources are only required at runtime.
    Runtime;
    
    /// All providers.
    public static final Set<Intend> ALL = EnumSet.allOf(Intend.class);
    
    /// All providers of resources that are (or can be) used by a project's
    /// generators ([Intend#Consume] and [Intend#Expose]).
    public static final Set<Intend> CONTRIBUTORS = EnumSet.of(Consume, Expose);
}
