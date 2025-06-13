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

import java.util.Objects;

/// Represents a request for a resource of a given type, optionally
/// with a given restriction on the resources to consider.
///
/// @param <T> the generic type
///
public class ResourceRequest<T extends Resource> {

    /// Restrict the resource to be returned.
    ///
    @SuppressWarnings("PMD.FieldNamingConventions")
    public enum Restriction {
        
        /// No restriction. Provide all resources of the requested type.
        None, 
        
        /// Request only resources that are exposed.
        Exposed
    }

    private final ResourceType<T> type;
    private final Restriction restriction;

    /// Instantiates a new resource request without any restriction.
    ///
    /// @param type the requested type
    ///
    public ResourceRequest(ResourceType<T> type) {
        this(type, Restriction.None);
    }

    /// Instantiates a new resource request with the given restriction.
    ///
    /// @param type the type
    /// @param restriction the restriction
    ///
    public ResourceRequest(ResourceType<T> type, Restriction restriction) {
        this.type = type;
        this.restriction = restriction;
    }

    /// Return the requested type.
    ///
    /// @return the resource type
    ///
    public ResourceType<T> type() {
        return type;
    }

    /// Checks if this request requests a resource of the given type.
    ///
    /// @param other the other
    /// @return true, if successful
    ///
    public boolean wants(ResourceType<?> other) {
        return type().isAssignableFrom(other);
    }
    
    /// Return the restriction.
    ///
    /// @return the restriction
    ///
    public Restriction restriction() {
        return restriction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(restriction, type);
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
        ResourceRequest<?> other = (ResourceRequest<?>) obj;
        return restriction == other.restriction
            && Objects.equals(type, other.type);
    }

    @Override
    public String toString() {
        return "ResourceRequest [type=" + type + ", restriction=" + restriction
            + "]";
    }

}
