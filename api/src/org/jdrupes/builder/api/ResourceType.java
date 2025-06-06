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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/// Represents a resource type.
///
/// @param <T> the resource type
///
public class ResourceType<T extends Resource> {

    /// The Java class files.
    public static final ResourceType<FileTree<ResourceFile>> RESOURCE_FILES
        = new ResourceType<>() {
        };

    /// Used to request cleanup.
    public static final ResourceType<Cleaniness> CLEANINESS
        = new ResourceType<>() {
        };

    private final Class<T> type;
    private final Class<? extends Resource> containedType;

    /// Instantiates a new resource type.
    ///
    /// @param type the type
    /// @param containedType the contained type
    ///
    public ResourceType(Class<T> type,
            Class<? extends Resource> containedType) {
        this.type = type;
        this.containedType = containedType;
    }

    /// Instantiates a new resource type, using the information from a
    /// derived class.
    ///
    @SuppressWarnings({ "unchecked", "PMD.AvoidCatchingGenericException" })
    protected ResourceType() {
        Type myType = getClass().getGenericSuperclass();
        try {
            Type resourceType
                = ((ParameterizedType) myType).getActualTypeArguments()[0];
            if (resourceType instanceof ParameterizedType genType) {
                type = (Class<T>) genType.getRawType();
                containedType = (Class<? extends Resource>) genType
                    .getActualTypeArguments()[0];
            } else {
                type = (Class<T>) resourceType;
                containedType = null;
            }
        } catch (Exception e) {
            throw new UnsupportedOperationException(
                "Could not derive resource type for " + myType, e);
        }
    }

    /// Return the type.
    ///
    /// @return the class
    ///
    public Class<T> type() {
        return type;
    }

    /// Return the contained type.
    ///
    /// @return the class<? extends resource>
    ///
    public Class<? extends Resource> containedType() {
        return containedType;
    }

    /// Checks if this is assignable from the other resource type.
    ///
    /// @param other the other
    /// @return true, if is assignable from
    ///
    @SuppressWarnings("PMD.SimplifyBooleanReturns")
    public boolean isAssignableFrom(ResourceType<?> other) {
        if (!type.isAssignableFrom(other.type)) {
            return false;
        }
        if (Objects.isNull(containedType)) {
            // If this is not a container but assignable, we're okay.
            return true;
        }
        if (Objects.isNull(other.containedType)) {
            // If this is a container but other is not, this should
            // have failed before.
            return false;
        }
        return containedType.isAssignableFrom(other.containedType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containedType, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!ResourceType.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        ResourceType<?> other = (ResourceType<?>) obj;
        return Objects.equals(containedType, other.containedType)
            && Objects.equals(type, other.type);
    }

    @Override
    public String toString() {
        return "ResourceType " + type.getSimpleName()
            + (containedType == null ? ""
                : "(" + containedType.getSimpleName() + ")");
    }

}
