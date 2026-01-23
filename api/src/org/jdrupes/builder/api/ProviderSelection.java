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

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/// The interface ProviderSelection.
///
public interface ProviderSelection {

    /// Only include the [ResourceProvider]s for which the filter
    /// evaluates to `true`.
    ///
    /// @param filter the filter
    /// @return the provider selection
    ///
    ProviderSelection filter(Predicate<ResourceProvider> filter);

    /// Exclude the given provider when fetching resources.
    ///
    /// @param provider the provider
    /// @return the bound resource query
    ///
    ProviderSelection without(ResourceProvider provider);

    /// Exclude providers of the given type when fetching resources.
    ///
    /// @param providerType the provider type
    /// @return the bound resource query
    ///
    ProviderSelection
            without(Class<? extends ResourceProvider> providerType);

    /// Register a callback for logging the provider invocation.
    ///
    /// @param hook the hook
    /// @return the bound resource query
    ///
    ProviderSelection onBeforeUse(Consumer<ResourceProvider> hook);

    /// Returns the providers with the requested intents from the project
    /// matching the defined filters.
    ///
    /// @param intents the intents
    /// @return the stream
    ///
    Stream<ResourceProvider> select(Set<Intent> intents);

    /// Returns the providers with the requested intents from the project
    /// matching the defined filters.
    ///
    /// @param intent the intent
    /// @param intents the intents
    /// @return the stream
    ///
    default Stream<ResourceProvider> select(Intent intent, Intent... intents) {
        return select(EnumSet.of(intent, intents));
    }

    /// Returns the requested resources using the providers with the
    /// requested intents from the project passed to constructor
    /// and the defined filters.
    ///
    /// @param <T> the requested resource type
    /// @param requested the resource request
    /// @return the stream
    ///
    <T extends Resource> Stream<T> resources(ResourceRequest<T> requested);

}
