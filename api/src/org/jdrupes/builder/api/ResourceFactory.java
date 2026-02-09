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

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

/// Defines both an interface for factories that create [Resource]s and
/// factory methods for invoking an appropriate factory.
///
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ResourceFactory {

    /// Returns a new resource with the given type, passing the given
    /// arguments to the constructor of the resource. The implementation
    /// uses [ServiceLoader] to find a [ResourceFactory] that creates the
    /// resource, i.e. that does not return `Optional.empty()` when
    /// [newResource] is called.
    ///
    /// @param <T> the generic resource type
    /// @param type the resource type
    /// @param project the project
    /// @param args the additional arguments
    /// @return the resource
    ///
    static <T extends Resource> T create(ResourceType<T> type,
            Project project, Object... args) {
        return StreamSupport.stream(
            ServiceLoader.load(ResourceFactory.class).spliterator(), true)
            .map(f -> f.newResource(type, project, args))
            .filter(Optional::isPresent).map(Optional::get).findFirst()
            .orElseThrow(() -> new ConfigurationException()
                .message("No resource factory for %s", type));
    }

    /// Short for `create(type, null, args)`.
    ///
    /// @param <T> the generic resource type
    /// @param type the resource type
    /// @param args the additional arguments
    /// @return the resource
    ///
    static <T extends Resource> T create(ResourceType<T> type,
            Object... args) {
        return create(type, null, args);
    }

    /// Returns a new resource of the given type if the factory instance
    /// can create it.
    ///
    /// @param <T> the generic resource type
    /// @param type the resource type
    /// @param project the project
    /// @param args the additional arguments
    /// @return the result. `Optional.empty()` if the resource cannot
    /// be created by the factory instance.
    ///
    <T extends Resource> Optional<T> newResource(ResourceType<T> type,
            Project project, Object... args);

}
