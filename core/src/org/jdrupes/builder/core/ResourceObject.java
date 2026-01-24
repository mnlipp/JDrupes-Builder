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

import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.Optional;
import org.jdrupes.builder.api.Proxyable;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceType;

/// A base class for [Resource]s.
///
public abstract class ResourceObject implements Resource, Proxyable {

    private final ResourceType<?> type;
    private String name;
    private boolean isLocked;

    /// Create a new instance.
    ///
    protected ResourceObject() {
        this.type = new ResourceType<>(getClass(), null);
    }

    /// Create a new instance.
    ///
    /// @param type the type
    ///
    protected ResourceObject(ResourceType<?> type) {
        this.type = Objects.requireNonNull(type);
    }

    /* default */ @SuppressWarnings("unchecked")
    static <T extends Resource> T createResource(ResourceType<T> type) {
        return (T) Proxy.newProxyInstance(type.rawType().getClassLoader(),
            new Class<?>[] { type.rawType(), Proxyable.class },
            new ForwardingHandler(new ResourceObject(type) {}));
    }

    @Override
    public ResourceType<?> type() {
        return type;
    }

    @Override
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /// Sets the name of the resource.
    ///
    /// @param name the name
    /// @return the resource object
    ///
    public final ResourceObject name(String name) {
        if (isLocked) {
            throw new IllegalStateException(
                "Name may only be set once immediately after creation.");
        }
        isLocked = true;
        this.name = name;
        return this;
    }

    @Override
    public int hashCode() {
        isLocked = true;
        return Objects.hash(type(), name());
    }

    @Override
    public boolean equals(Object obj) {
        isLocked = true;
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return (obj instanceof ResourceObject other)
            && Objects.equals(type(), other.type())
            && Objects.equals(name(), other.name());
    }

    @Override
    public String toString() {
        return type() + name().map(n -> ":" + n).orElse("")
            + " (" + asOfLocalized() + ")";
    }
}
