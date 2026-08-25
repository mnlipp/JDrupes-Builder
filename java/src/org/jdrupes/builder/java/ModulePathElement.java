/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
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

package org.jdrupes.builder.java;

/// Marker interface for code contributions that contain a module descriptor
/// and can be placed on the module-path (--module-path) during compilation
/// or run-time.
///
/// A resource implementing this interface explicitly declares itself as a
/// named module. Even without this marker, a [CodeContribution] can be
/// detected as modular, see [CodeContribution#isModular()].
///
public interface ModulePathElement extends CodeContribution {

    @Override
    default boolean isModular() {
        return true;
    }

}
