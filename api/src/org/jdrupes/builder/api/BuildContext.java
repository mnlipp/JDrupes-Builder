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

    /// The relative path to the JDrupes Builder directory from a 
    /// project directory.
    ///
    /// @return the path
    ///
    Path jdbldDirectory();

    /// The command line as processed by Apache Commons CLI.
    ///
    /// @return the string[]
    ///
    CommandLine commandLine();

    /// Obtains the resource stream for the given resource from the
    /// given provider. The result from invoking the provider is
    /// evaluated asynchronously and cached. Only when the returned
    /// stream is terminated will the invocation block until the
    /// result from the provider becomes available.
    ///
    /// @param <T> the resource type
    /// @param provider the provider
    /// @param request the request
    /// @return the results
    ///
    <T extends Resource> Stream<T> get(ResourceProvider provider,
            ResourceRequest<T> request);
}
