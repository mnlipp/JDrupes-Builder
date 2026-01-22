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

package org.jdrupes.builder.api;

import java.util.stream.Stream;

/// This interface defines the access to a provider's resources.
/// Resources are provided by calling [resources][#resources] with a
/// [ResourceRequest]. The interface also serves as a factory for
/// creating the resource requests for this invocation.
///
public interface ResourceProvider {

    /// Returns resources provided by this resource provider. Short for
    /// `context().resources(this, request)`.
    ///
    /// @param <T> the generic type
    /// @param requested the request
    /// @return the stream
    ///
    default <T extends Resource> Stream<T>
            resources(ResourceRequest<T> requested) {
        return context().resources(this, requested);
    }

    /// Convenience method to access the build context which is sometimes
    /// needed in the context of resource requests.
    ///
    /// @return the builder configuration
    ///
    BuildContext context();

    /// Create a new request for the given resource.
    ///
    /// @param <T> the resource type
    /// @param type the type
    /// @return the resource request
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    <T extends Resource> ResourceRequest<T> of(ResourceType<? extends T> type);

    /// Create a new request for the given resource type.
    ///
    /// @param <T> the resource type
    /// @param requested the requested
    /// @return the resource request
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    default <T extends Resource> ResourceRequest<T> of(Class<T> requested) {
        return of(new ResourceType<>(requested, null));
    }
}
