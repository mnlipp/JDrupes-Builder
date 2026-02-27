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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;

/// The context of a build.
///
public interface BuildContext extends AutoCloseable {

    /// The key for specifying the builder directory in the properties file.
    String JDBLD_DIRECTORY = "jdbldDirectory";

    /// The key for specifying the builder version in the properties file.
    String JDBLD_VERSION = "jdbldVersion";

    /// The key for specifying the build extensions in the properties file.
    String BUILD_EXTENSIONS = "buildExtensions";

    /// The key for specifying the extensions snapshot repository in the
    /// properties file.
    @SuppressWarnings("PMD.LongVariable")
    String EXTENSIONS_SNAPSHOT_REPOSITORY = "extensionsSnapshotRepository";

    /// The key for specifying the common configuration and cache directory.
    String JDBLD_COMMON_DIRECTORY = "jdbldCommonDirectory";

    /// Returns the relative path from a project directory to
    /// the JDrupes Builder directory.
    ///
    /// @return the path
    ///
    default Path jdbldDirectory() {
        return Path.of(property(JDBLD_DIRECTORY, "_jdbld"));
    }

    /// The path to the common cache directory. This may be used by
    /// providers to cache information that is shared between projects.
    /// Providers must create a sub-directory of this directory, preferably
    /// with the same name FQN as the provider.
    ///
    /// @return the path
    ///
    default Path commonCacheDirectory() {
        return Path.of(property(JDBLD_COMMON_DIRECTORY,
            System.getProperty("user.home"))).resolve(".jdbld/cache");
    }

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

    /// Returns the status line for the current thread. The status line
    /// may be used by [ResourceProvider]s to indicate progress during
    /// the execution of [ResourceProviderSpi#provide(ResourceRequest)].
    /// A [StatusLine] is automatically allocated by the context when
    /// [#resources] is invoked.
    /// 
    /// When called while not executing [#resources], this method
    /// returns a dummy status line that discards all updates.
    ///
    /// @return the status line
    ///
    StatusLine statusLine();

    /// Returns the [PrintStream] for the standard output.
    ///
    /// @return the prints the stream
    ///
    PrintStream out();

    /// Returns a [PrintStream] for errors. The data is sent to the
    /// standard output stream as with [#out], but it is marked,
    /// typically in red, to indicate an error.
    ///
    /// @return the prints the stream
    ///
    PrintStream error();

    /// Close the context. The re-declaration of this method removes
    /// the [IOException], which is never thrown.
    ///
    @Override
    void close();
}
