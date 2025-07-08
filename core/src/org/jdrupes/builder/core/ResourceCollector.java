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

import java.util.stream.Stream;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;

/// A provider of resources to be included in a project. This
/// implementation can be used for all kinds of resources. Usually
/// language specific packages derive specializations that bind
/// this class to a specific type of resource. These specializations
/// often also offer methods that ease the specification of resources
/// to be included.
///
/// @param <T> the resource type type
///
public class ResourceCollector<T extends Resource>
        extends AbstractGenerator<T> {

    private final ResourceType<T> type;
    private final Resources<T> resources;

    /// Instantiates a new resources collector.
    ///
    /// @param project the project
    /// @param type the type of resources to collect
    ///
    @SuppressWarnings({ "PMD.ConstructorCallsOverridableMethod", "unchecked" })
    public ResourceCollector(Project project, ResourceType<T> type) {
        super(project);
        this.type = type;
        resources
            = project().resource(new ResourceType<>(Resources.class, type) {});
    }

    /// Adds the given file tree with resource directories.
    ///
    /// @param resources the resources
    /// @return the resources collector
    ///
    public final ResourceCollector<T> add(T resources) {
        this.resources.add(resources);
        return this;
    }

    /// Adds the given file trees with resource directories.
    ///
    /// @param resources the resources
    /// @return the resources collector
    ///
    public final ResourceCollector<T> add(Stream<T> resources) {
        this.resources.addAll(resources);
        return this;
    }

    /// Return the resources to collect.
    ///
    /// @return the resources
    ///
    public final Resources<T> resources() {
        return resources;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Resource> Stream<R>
            provide(ResourceRequest<R> requested) {
        if (!requested.type().containedType().isAssignableFrom(type)) {
            return Stream.empty();
        }
        return (Stream<R>) resources.stream();
    }

}
