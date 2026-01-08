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

/// Marker interface for merged test projects. Merged test projects are
/// conceptually merged with their parent project. Implementing this
/// interface has an effect on the behavior of some of the project's methods
/// (see e.g. [Project#generator(Generator)]). It may also have an effect
/// on providers (see the respective provider's documentation).
/// 
/// Projects that implement this interface must specify the project under
/// test as their parent project. They must not specify a directory as
/// it will automatically be set to the parent project's directory. 
///
public interface MergedTestProject {

}
