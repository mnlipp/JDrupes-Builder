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

package org.jdrupes.builder.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;

/// Represents a request for [Resource]s of a specified type.
/// The specified type provides two kinds of type information:
///
/// 1. The type of the [Resource]s that are actually provided.
/// 2. The type of the "context" in which the [Resource]s are to be provided.
///
/// As an example, consider requests for a compile time and a runtime
/// classpath. In both cases, the actually provided [Resource]s are
/// of type "classpath element". However, depending on the kind of
/// classpath, a [ResourceProvider] may deliver different collections of
/// instances of "classpath elements". So instead of requesting
/// "classpath element", 
///
/// Not all requested resource types require context information. For
/// example, a request for [Cleanliness] usually refers to all resources
/// that a [Generator] has created and does not depend on a context.
/// However, in order to keep the API simple, the context is always
/// required. 
///
/// @param <T> the generic type
///
public class DefaultResourceRequest<T extends Resource>
        implements ResourceRequest<T> {

    private final ResourceType<? extends Resources<T>> type;
    private final List<Project> queried = new ArrayList<>();

    /// Instantiates a new resource request without any restriction.
    ///
    /// @param type the requested type
    ///
    public DefaultResourceRequest(ResourceType<? extends Resources<T>> type) {
        this.type = type;
    }

    @Override
    public <R extends Resources<T>> ResourceRequest<T> widened(
            @SuppressWarnings("rawtypes") Class<? extends Resources> type) {
        return new DefaultResourceRequest<>(type().widened(type));
    }

    @Override
    public ResourceType<? extends Resources<T>> type() {
        return type;
    }

    @Override
    public boolean accepts(ResourceType<?> other) {
        return type().isAssignableFrom(other);
    }

    @Override
    public boolean collects(ResourceType<?> type) {
        return Optional.ofNullable(type().containedType())
            .map(ct -> ct.isAssignableFrom(type)).orElse(false);
    }

    /* default */ List<Project> queried() {
        return queried;
    }

    /* default */ void queried(Project project) {
        queried.add(project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
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
        DefaultResourceRequest<?> other = (DefaultResourceRequest<?>) obj;
        return Objects.equals(type, other.type);
    }

    @Override
    public String toString() {
        return "ResourceRequest<" + type + ">";
    }

}
