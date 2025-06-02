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

import java.util.Objects;

import org.jdrupes.builder.api.Resource;

/// A base class for [Resource]s.
///
public abstract class ResourceObject implements Resource {

    private final Class<? extends Resource> type;

    /// Create a new instance
    ///
    protected ResourceObject() {
        type = getClass();
    }

    /// Create a new instance.
    ///
    /// @param type the type
    ///
    protected ResourceObject(Class<? extends Resource> type) {
        this.type = type;
    }

    @Override
    public Class<? extends Resource> type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ResourceObject other = (ResourceObject) obj;
        return Objects.equals(type(), other.type());
    }

    @Override
    public String toString() {
        return "Resource of kind " + type().getSimpleName();
    }
}
