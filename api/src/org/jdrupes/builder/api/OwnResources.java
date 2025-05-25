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

/// A class for requesting the [Resource]s of a given kind that are
/// owned (generated) by a provider.
///
public class OwnResources extends ResourceObject {

    /// Create a new instance
    ///
    protected OwnResources() {
        // kind is already set
    }

    /// Create a new instance with the given kind
    ///
    protected OwnResources(String kind) {
        super(kind);
    }

    /// Create a new instance with the given kind
    ///
    /// @param kind the kind
    /// @return the resource
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    public static Resource of(String kind) {
        return new OwnResources(kind);
    }

    @Override
    public String toString() {
        return "Own of kind " + kind();
    }
}
