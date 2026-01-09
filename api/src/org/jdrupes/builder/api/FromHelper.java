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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/// Helper class for implementing [Project#from].
///
public class FromHelper {
    private final BuildContext context;
    private final Stream<ResourceProvider> providers;
    private final Set<ResourceProvider> excluded = new HashSet<>();
    private final Set<Class<? extends ResourceProvider>> excludedTypes
        = new HashSet<>();
    private Consumer<ResourceProvider> providerLog = _ -> {
    };

    /// Initializes a new from helper.
    ///
    /// @param context the context
    /// @param providers the providers
    ///
    public FromHelper(BuildContext context,
            Stream<ResourceProvider> providers) {
        this.context = context;
        this.providers = providers;
    }

    /// Exclude the given provider when requesting resources.
    ///
    /// @param provider the provider
    /// @return the from helper
    ///
    public FromHelper without(ResourceProvider provider) {
        excluded.add(provider);
        return this;
    }

    /// Exclude providers of the given type when requesting resources.
    ///
    /// @param providerType the provider type
    /// @return the from helper
    ///
    public FromHelper without(Class<? extends ResourceProvider> providerType) {
        excludedTypes.add(providerType);
        return this;
    }

    /// Register a callback for logging the providers used.
    ///
    /// @param providerLog the provider log
    /// @return the from helper
    ///
    public FromHelper log(Consumer<ResourceProvider> providerLog) {
        this.providerLog = providerLog;
        return this;
    }

    /// Returns the requested resources using the context and providers 
    /// passed to the record's constructor.
    ///
    /// @param <T> the generic type
    /// @param request the request
    /// @return the stream
    ///
    public <T extends Resource> Stream<T> get(ResourceRequest<T> request) {
        return providers.filter(p -> !excluded.contains(p))
            .filter(p -> excludedTypes.stream()
                .filter(t -> t.isAssignableFrom(p.getClass())).findAny()
                .isEmpty())
            .peek(providerLog::accept)
            .flatMap(provider -> context.get(provider, request));
    }
}
