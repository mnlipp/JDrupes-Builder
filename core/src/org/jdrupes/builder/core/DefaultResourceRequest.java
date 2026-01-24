/*
 * JDrupes Builder
 * Copyright (C) 2025, 2026 Michael N. Lipp
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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jdrupes.builder.api.Intent;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;

/// An implementation of [ResourceRequest]. 
///
/// @param <T> the resource type
///
public class DefaultResourceRequest<T extends Resource>
        implements ResourceRequest<T> {

    private final ResourceType<? extends T> type;
    private Project[] queried;
    private Set<Intent> uses;
    private String name;

    /// Instantiates a new resource request without any intents.
    ///
    /// @param type the requested type
    ///
    /* default */ DefaultResourceRequest(ResourceType<? extends T> type) {
        this.type = Objects.requireNonNull(type);
        uses = EnumSet.noneOf(Intent.class);
        queried = new Project[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultResourceRequest<T> clone() {
        try {
            // This class is immutable
            return (DefaultResourceRequest<T>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ResourceType<? extends T> type() {
        return type;
    }

    @Override
    public ResourceRequest<T> withName(String name) {
        var result = clone();
        result.name = name;
        return result;
    }

    @Override
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    @Override
    public ResourceRequest<T> using(Set<Intent> intents) {
        var result = clone();
        result.uses = Objects.requireNonNull(intents);
        return result;
    }

    @Override
    public Set<Intent> uses() {
        return EnumSet.copyOf(uses);
    }

    @Override
    public boolean accepts(ResourceType<?> type) {
        return this.type.isAssignableFrom(type);
    }

    @Override
    public boolean requires(ResourceType<?> type) {
        return type.isAssignableFrom(this.type);
    }

    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    /* default */ Project[] queried() {
        return queried;
    }

    /* default */ DefaultResourceRequest<T> queried(Project project) {
        var newQueried = Arrays.copyOf(queried, queried.length + 1);
        newQueried[newQueried.length - 1] = Objects.requireNonNull(project);
        var result = clone();
        result.queried = newQueried;
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, uses);
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
        return Objects.equals(type, other.type)
            && Objects.equals(name, other.name)
            && Objects.equals(uses, other.uses);
    }

    @Override
    public String toString() {
        return "ResourceRequest<" + type + ">"
            + name().map(n -> ":" + n).orElse("") + " [" + uses.stream()
                .map(Intent::toString).collect(Collectors.joining(", "))
            + "]";
    }

}
