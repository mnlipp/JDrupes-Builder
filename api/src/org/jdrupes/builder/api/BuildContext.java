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

import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;

/// The context of a build.
///
public interface BuildContext {

    /// Returns the relative path from a project directory to
    /// the JDrupes Builder directory.
    ///
    /// @return the path
    ///
    Path jdbldDirectory();

    /// The command line as processed by Apache Commons CLI.
    ///
    /// @return the parsed command line
    ///
    CommandLine commandLine();

    /// Obtains the stream of resources of the given type from the
    /// given provider. The result from invoking the provider is
    /// evaluated asynchronously and cached. Only when the returned
    /// stream is terminated will the invocation block until the
    /// result from the provider becomes available.
    /// 
    /// To avoid duplicate invocations of a non-project provider,
    /// any intends are removed from the request before such a
    /// provider is invoked.
    ///
    /// @param <T> the resource type
    /// @param provider the provider
    /// @param request the request
    /// @return the results
    ///
    <T extends Resource> Stream<T> resources(ResourceProvider provider,
            ResourceRequest<T> request);

    /// Returns the value of the given property. Properties are defined by
    /// (in order of precedence):
    ///   1. command line options
    ///   2. the file `.jdbld.properties` in the directory of the
    ///      root project
    ///   3. the file `.jdbld/jdbld.properties` in the user's home directory
    ///
    /// @param name the name
    /// @param defaultValue the default value
    /// @return the string
    ///
    String property(String name, String defaultValue);
}
