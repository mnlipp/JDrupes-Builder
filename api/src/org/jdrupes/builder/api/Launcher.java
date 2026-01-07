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

/// Defines the methods provided by a launcher.
///
public interface Launcher {

    /// Provide the requested resources from the root project.
    ///
    /// @param <T> the requested type
    /// @param requested the request
    /// @return the stream
    ///
    <T extends Resource> Stream<T> provide(ResourceRequest<T> requested);

    /// Return the root project.
    ///
    /// @return the root project
    ///
    RootProject rootProject();
}