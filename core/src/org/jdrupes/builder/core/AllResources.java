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

package org.jdrupes.builder.core;

import org.jdrupes.builder.api.Resource;

/// A class for requesting [Resource]s.
///
public class AllResources implements Resource {

    private String kind = KIND_UNKNOWN;

    /// Create a new instance
    ///
    protected AllResources() {
        // kind is already set
    }

    /// Create a new instance with the given kind
    ///
    protected AllResources(String kind) {
        this.kind = kind;
    }

    /// Create a new instance with the given kind
    ///
    /// @param kind the kind
    /// @return the resource
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    public static Resource of(String kind) {
        return new AllResources(kind);
    }

    @Override
    public String kind() {
        return kind;
    }

    @Override
    public String toString() {
        return "Resource of kind " + kind;
    }
}
