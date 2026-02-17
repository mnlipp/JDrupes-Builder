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

/// A "phony resource" that (when requested) causes a generator to remove
/// its generated outputs.
/// 
/// Note that requesting [Cleanliness] may leave [ResourceProvider]s in an
/// inconsistent state. The reason is that providers can cache resources
/// that they depend on. If a [Cleanliness] resource is requested, all
/// cleaned resources would have to be removed from these caches, which
/// cannot be done reliably.
/// 
/// Therefore, the build project should be discarded after [Cleanliness]
/// has been requested and re-generated. 
///
public interface Cleanliness extends Resource {
}
