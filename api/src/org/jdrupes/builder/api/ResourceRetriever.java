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

/// The interface ResourceRetriever, see [Generator].
///
public interface ResourceRetriever {

    /// Adds the given provider(s) as source for resources.
    ///
    /// @param providers the provider to add
    /// @return the resource retriever
    ///
    ResourceRetriever addFrom(ResourceProvider... providers);

    /// Adds the given providers as sources for resources. The stream
    /// must not be terminated before the generators' provide method
    /// is invoked.
    ///
    /// @param providers the providers to retrieve resources from
    /// @return the resource retriever
    ///
    ResourceRetriever addFrom(Stream<ResourceProvider> providers);

}
