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

import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceObject;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;

/// Represents a set of resources. Resources are added by [add].
/// The [stream] method returns a stream of the added resources.
/// The [stream] method preserves the order in which the resources
/// were added.
///
/// @param <T> the type of the contained resources
///
public class DefaultResources<T extends Resource> extends ResourceObject
        implements Resources<T> {

    /// The log.
    protected final Logger log = Logger.getLogger(getClass().getName());
    private final Set<T> content;

    /// Instantiates a new resource set.
    ///
    /// @param type the type of this instance as resource
    ///
    protected DefaultResources(ResourceType<?> type) {
        super(type);
        content = new LinkedHashSet<>();
    }

    /* default */ @SuppressWarnings("unchecked")
    static <T extends Resources<?>> T create(ResourceType<T> type) {
        return (T) Proxy.newProxyInstance(type.type().getClassLoader(),
            new Class<?>[] { type.type() },
            new ForwardingHandler(new DefaultResources<>(type)));
    }

    @Override
    public Resources<T> add(T resource) {
        content.add(resource);
        return this;
    }

    @Override
    public Stream<T> stream() {
        return content.stream();
    }

    @Override
    public Resources<T> clear() {
        content.clear();
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(content);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DefaultResources<?> other = (DefaultResources<?>) obj;
        return Objects.equals(content, other.content);
    }

    @Override
    public String toString() {
        return type().toString() + " (" + asOfLocalized()
            + ") with " + content.size() + " elements";
    }

}
