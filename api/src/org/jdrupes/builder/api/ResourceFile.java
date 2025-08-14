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

/// A [FileResource] that represents a resource provided by a [Project].
///
/// Admittedly, the name is a bit confusing. Usually, the purpose of file
/// artifacts provided by a project can easily be derived from their name
/// e.g. `ZipFile` or `JarFile`. This interface represents an artifact that
/// has no specific format (as the `ZipFile` has) or purpose (as a
/// `JarFile` has). It's just a file providing some kind of resource,
/// which makes it a `ResourceFile`.
///
public interface ResourceFile extends FileResource {

}
