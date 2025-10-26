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

/// Helper class for implementing [Project#from].
///
/// @param context the context
/// @param providers the providers
///
public record FromHelper(BuildContext context,
        Stream<ResourceProvider> providers) {

    /// Returns the requested resources using the context and providers 
    /// passed to the record's constructor.
    ///
    /// @param <T> the generic type
    /// @param request the request
    /// @return the stream
    ///
    public <T extends Resource> Stream<T> get(ResourceRequest<T> request) {
        return providers.flatMap(provider -> context.get(provider, request));
    }

}
