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

import java.nio.file.Path;
import java.util.stream.Stream;

/// Implemented by [ResourceProvider]s that require resources to be
/// available without needing them for a specific purpose. The need
/// for this usually arises when a [ResourceProvider] is scriptable
/// and the required resources depend on the script.
/// 
/// Such resource providers should use the methods from this interface
/// to register the required resources, thus ensuring a consistent
/// API.
///
public interface RequiredResourceSupport extends ResourceProvider {

    /// Adds the given [Stream] of resources to the required resources.
    ///
    /// @param resources the resources
    /// @return the provider
    ///
    RequiredResourceSupport required(Stream<? extends Resource> resources);

    /// Convenience method that adds a [FileTree] to the required resources.
    /// If `root` is relative, it is resolved against the project's
    /// directory. 
    ///
    /// @param root the root
    /// @param pattern the pattern
    /// @return the provider
    ///
    RequiredResourceSupport required(Path root, String pattern);

    /// Convenience method that adds a [FileResource] to the required
    /// resources. If `path` is relative, it is resolved against the
    /// project's directory. 
    ///
    /// @param file the file
    /// @return the provider
    ///
    RequiredResourceSupport required(Path file);
}
