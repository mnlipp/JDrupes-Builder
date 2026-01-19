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
import java.util.Set;
import org.jdrupes.builder.api.Intent;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;

/// An implementation of [ResourceRequest]. 
///
/// @param <T> the generic type
///
public class DefaultResourceRequest<T extends Resource>
        implements ResourceRequest<T> {

    private final ResourceType<? extends T> type;
    private final Project[] queried;
    private Set<Intent> uses;

    /// Instantiates a new resource request without any intents.
    ///
    /// @param type the requested type
    ///
    /* default */ DefaultResourceRequest(ResourceType<? extends T> type) {
        this(type, EnumSet.noneOf(Intent.class), new Project[0]);
    }

    @SuppressWarnings("PMD.UseVarargs")
    private DefaultResourceRequest(ResourceType<? extends T> type,
            Set<Intent> using, Project[] queried) {
        this.type = type;
        uses = using;
        this.queried = queried;
    }

    @Override
    public ResourceRequest<T> copyWithType() {
        return new DefaultResourceRequest<>(type(),
            EnumSet.noneOf(Intent.class), queried);
    }

    @Override
    public ResourceRequest<T> using(Set<Intent> intents) {
        uses = intents;
        return this;
    }

    @Override
    public Set<Intent> uses() {
        return EnumSet.copyOf(uses);
    }

    @Override
    public ResourceType<? extends T> type() {
        return type;
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
        newQueried[newQueried.length - 1] = project;
        return new DefaultResourceRequest<>(type(), uses, newQueried);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, uses);
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
            && Objects.equals(uses, other.uses);
    }

    @Override
    public String toString() {
        return "ResourceRequest<" + type + ">";
    }

}
