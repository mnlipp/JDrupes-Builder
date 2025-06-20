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

/// Attributes the relationship between a [Project] ("this project")
/// and an associated [ResourceProvider].
///
/// <table>
///   <thead>
///     <tr>
///       <td></td><td></td><th colspan="2">Input for generators</th>
///     </tr>
///     <tr>
///       <td></td><td></td><th> yes </th><th> no </th>
///     </tr>
///   </thead>
///   <tbody>
///     <tr>
///       <th rowspan="2">Provided</th><th> yes </th><td></td><td></td>
///     </tr>
///     <tr>
///       <th> no </th><td></td><td></td>
///     </tr>
///   </tbody>
/// </table>
///
/// | Intend   ||
/// |  Consume | Don't consume |
/// |:--------:|:-------------:|
/// |  Expose  | Don't expose  |
///
@SuppressWarnings("PMD.FieldNamingConventions")
public enum Intend {
    
    /// The project should ignore the resources from the provider.
    /// This is the default relationship between a project and its
    /// sub projects.
    Ignore,
    
    /// The project consumes the resources from the associated
    /// provider, but it does not expose them, i.e. the project
    /// in its role as provider does not provide them to others.
    Consume,
    
    /// The resources from the provider are forwarded (exposed) to
    /// other projects that have this project as a dependency but
    /// are not used (consumed) by this project.
    Forward,
    
    /// The project consumes the resources from the associated
    /// provider and makes them available to other projects that
    /// have this project as a dependency.
    Expose,
    
    /// The resources from the associated provider are genuinely
    /// provided by this project, i.e. supplied by this project.
    /// This is the default relationship between a project and its
    /// generators. It implies that the resources obtained through
    /// this dependency are exposed to projects that have this
    /// project as a dependency.
    Supply;
    
    /// All providers.
    public static final Set<Intend> ALL = EnumSet.allOf(Intend.class);
    
    /// All providers of resources that are (or can be) used by a project's
    /// generators ([Intend#Consume] and [Intend#Expose]).
    public static final Set<Intend> PROVIDED = EnumSet.of(Consume, Expose);
}
