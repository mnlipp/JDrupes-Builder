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

/// The representation of a file tree. A file tree is a collection
/// of [FileResource]s that are contained in a directory hierarchy
/// with a common root.
///
/// Implementations of this interface must provide a [ResourceFactory]
/// that supports the invocation of [ResourceFactory#create] with
/// arguments
///
/// * [Project] the project
/// * [Path] the root directory
/// * [String] the pattern
/// * `boolean` whether to include directories (optional, defaults to `false`)
///
/// Implementations of this interface must ensure that the content
/// of the file tree is not evaluated before a terminal operation
/// is performed on the [Stream] returned by [#entries]. The delayed
/// evaluation includes resolving a relative path for root against
/// the project's directory.
///
/// @param <T> the contained type
///
public interface FileTree<T extends FileResource> extends Resources<T> {

    /// Returns the root of the file tree containing the files.
    ///
    /// @param relativize whether to return a path relative to the project's
    /// directory
    /// @return the path
    ///
    Path root(boolean relativize);

    /// Returns the root of the file tree searched for files
    /// as an absolute path.
    ///
    /// @return the path
    ///
    default Path root() {
        return root(false);
    }

    /// Returns the paths of the files in this file tree relative to
    /// its root.
    ///
    /// @return the stream
    ///
    Stream<Path> entries();

    /// Re-scans the file tree for changes.
    ///
    /// @return the file tree
    ///
    @Override
    FileTree<T> clear();

    /// Deletes all files in this file tree and directories that are
    /// empty after deletion of the files (expect for root, which is
    /// not deleted).
    ///
    /// @return the file tree
    ///
    FileTree<T> delete();
}
