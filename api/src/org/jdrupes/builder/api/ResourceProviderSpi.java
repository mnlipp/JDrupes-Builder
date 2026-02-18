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

package org.jdrupes.builder.api;

import java.util.stream.Stream;

/// This interface must be made available to the [BuildContext]
/// by implementations of resource providers.
///
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ResourceProviderSpi {

    /// Provide the requested resources. This method is not intended
    /// to be invoked directly. Rather, it must be invoked via
    /// [BuildContext#resources].
    /// 
    /// When properly invoked through [BuildContext#resources], this
    /// method is never invoked twice for the same request (unless there
    /// has been a request for [Cleanliness] in between). The method
    /// may, however, be invoked concurrently for different requests.
    /// 
    /// Providers that evaluate all potentially provided resources anyway
    /// and return only a subset for some actually requested
    /// [ResourceType]s should therefore invoke themselves through
    /// [ResourceProvider#resources] with a request for all resources and
    /// filter the (automatically cached) result.
    /// 
    /// Special care must be taken when handling resource type hierarchies.
    /// If [ResourceType] B extends ResourceType A and the provider
    /// provides ResourceType B, it must also provide its resources
    /// in response to a request for ResourceType A. However, the caching
    /// mechanism is unaware of relationships between resource types.
    /// Requests for A and B are therefore forwarded independently.
    /// To avoid duplicate evaluation, the provider must map a request
    /// for A to a request for B (via [ResourceProvider#resources]).   
    ///
    /// @param <T> the type of the requested (and provided) resource
    /// @param request the request for resources
    /// @return the provided resource(s) as stream
    ///
    <T extends Resource> Stream<T> provide(ResourceRequest<T> request);
}
