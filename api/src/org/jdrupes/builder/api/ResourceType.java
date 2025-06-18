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
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/// A special kind of type token for representing a resource type.
/// The method [rawType()] returns the type as [Class]. If this class
/// if derived from [Resources], [containedType()] returns the
/// [ResourceType] of the contained elements.
///
/// Beware of automatic inference of type arguments. The inferred
/// type arguments will usually be super classes of what you expect.
///
/// @param <T> the resource type
///
public class ResourceType<T extends Resource> {

    /// Used to request cleanup.
    @SuppressWarnings("PMD.FieldNamingConventions")
    public static final ResourceType<
            Cleaniness> Cleaniness = new ResourceType<>() {};

    private final Class<T> type;
    private final ResourceType<?> containedType;

    /// Instantiates a new resource type.
    ///
    /// @param type the type
    /// @param containedType the contained type
    ///
    public ResourceType(Class<T> type, ResourceType<?> containedType) {
        this.type = type;
        this.containedType = containedType;
    }

    @SuppressWarnings("unchecked")
    private ResourceType(Type type) {
        if (type instanceof WildcardType wType) {
            type = wType.getUpperBounds()[0];
            if (Object.class.equals(type)) {
                type = Resource.class;
            }
        }
        if (type instanceof ParameterizedType pType && Resources.class
            .isAssignableFrom((Class<?>) pType.getRawType())) {
            this.type = (Class<T>) pType.getRawType();
            var argType = pType.getActualTypeArguments()[0];
            if (argType instanceof ParameterizedType pArgType) {
                containedType = new ResourceType<>(pArgType);
            } else {
                var subType = pType.getActualTypeArguments()[0];
                containedType = new ResourceType<>(subType);
            }
            return;
        }

        // If type is not a parameterized type, its super or one of its
        // interfaces may be.
        this.type = (Class<T>) type;
        this.containedType = Stream.concat(
            Optional.ofNullable(((Class<?>) type).getGenericSuperclass())
                .stream(),
            getAllInterfaces((Class<?>) type).map(Class::getGenericInterfaces)
                .map(Arrays::stream).flatMap(s -> s))
            .filter(t -> t instanceof ParameterizedType pType && Resources.class
                .isAssignableFrom((Class<?>) pType.getRawType()))
            .map(t -> (ParameterizedType) t).findFirst()
            .map(t -> new ResourceType<>(Resources.class,
                new ResourceType<>(t).containedType()))
            .orElseGet(() -> new ResourceType<>(Resources.class, null))
            .containedType();
    }

    /// Gets all interfaces that the given class implements,
    /// including the class itself.
    ///
    /// @param clazz the clazz
    /// @return all interfaces
    ///
    public static Stream<Class<?>> getAllInterfaces(Class<?> clazz) {
        return Stream.concat(Stream.of(clazz),
            Arrays.stream(clazz.getInterfaces())
                .map(ResourceType::getAllInterfaces).flatMap(s -> s));
    }

    /// Instantiates a new resource type, using the information from a
    /// derived class.
    ///
    @SuppressWarnings({ "unchecked", "PMD.AvoidCatchingGenericException",
        "rawtypes" })
    protected ResourceType() {
        Type resourceType = getClass().getGenericSuperclass();
        try {
            Type theResource = ((ParameterizedType) resourceType)
                .getActualTypeArguments()[0];
            var tempType = new ResourceType(theResource);
            type = tempType.rawType();
            containedType = tempType.containedType();
        } catch (Exception e) {
            throw new UnsupportedOperationException(
                "Could not derive resource type for " + resourceType, e);
        }
    }

    /// Return the type.
    ///
    /// @return the class
    ///
    public Class<T> rawType() {
        return type;
    }

    /// Return the contained type or `null`, if the resource is not
    /// a container.
    ///
    /// @return the type
    ///
    public ResourceType<?> containedType() {
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

    /// Returns a new [ResourceType] with the type (`this.type()`)
    /// widened to the given type. While this method may be invoked
    /// for any [ResourceType], it is intended to be used for
    /// containers (`ResourceType<Resources<?>>`) only.
    ///
    /// @param <R> the new raw type
    /// @param type the desired super type. This should actually be
    /// declared as `Class <R>`, but there is no way to specify a 
    /// parameterized type as actual parameter.
    /// @return the new resource type
    ///
    public <R extends Resource> ResourceType<R> widened(
            Class<? extends Resource> type) {
        if (!type.isAssignableFrom(this.type)) {
            throw new IllegalArgumentException("Cannot replace "
                + this.type + " with " + type + " because it is not a "
                + "super class");
        }
        if (Resources.class.isAssignableFrom(this.type)
            && !Resources.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot replace container"
                + " type " + this.type + " with non-container type " + type);
        }
        @SuppressWarnings("unchecked")
        var result = new ResourceType<>((Class<R>) type, containedType);
        return result;
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
        return type.getSimpleName() + (containedType == null ? ""
            : "(" + containedType + ")");
    }

}
