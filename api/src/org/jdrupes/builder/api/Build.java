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

import java.util.stream.Stream;

/// A build is the collection of all data related to building all projects.
///
/// In particular, it holds the global configuration information for the
/// build, as well as a cache of all resources that have been requested
/// from the [ResourceProvider]s during the build.
/// 
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface Build {

    /// Provides the resource stream for the given resource from the
    /// given provider. The result from invoking the provider is
    /// evaluated asynchronously and cached. Only when the stream
    /// returned is terminated will the invocation block until the
    /// result from the provider becomes available.
    ///
    /// @param <T> the requested resource type
    /// @param requested the requested resource
    /// @return the stream of resources
    ///
    <T extends Resource> Stream<T> provide(ResourceProvider<T> provider,
            Resource requested);

}
