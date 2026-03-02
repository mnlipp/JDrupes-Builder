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

    /// Includes directories in the file tree if they match the pattern
    /// used when creating the file tree, and are not excluded (i.e. don't 
    /// match the exclude pattern).
    ///
    /// @return the file tree
    ///
    FileTree<T> withDirectories();

    /// Add a file name pattern to exclude from the tree.
    ///
    /// @param pattern the pattern
    /// @return the file tree
    ///
    FileTree<T> exclude(String pattern);

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
    /// empty after deletion of the files including the root directory.
    ///
    @Override
    void cleanup();

    /// Creates a new general file tree from the given project and path.
    ///
    /// @param project the project
    /// @param directory the root of the file tree relative to the
    /// project's directory (or an absolute path)
    /// @param pattern the pattern
    /// @return the file tree
    ///
    @SuppressWarnings("PMD.UseDiamondOperator")
    static FileTree<FileResource> from(
            Project project, Path directory, String pattern) {
        return ResourceFactory.create(
            new ResourceType<FileTree<FileResource>>() {}, project,
            project != null ? project.directory().resolve(directory)
                : directory,
            pattern);
    }

    /// Creates a new file tree with elements of the given type from the
    /// given project and path.
    ///
    /// @param <T> the generic type
    /// @param project the project
    /// @param directory the root of the file tree relative to the
    /// project's directory (or an absolute path)
    /// @param pattern the pattern
    /// @param type the type
    /// @return the file tree
    ///
    @SuppressWarnings("unchecked")
    static <T extends FileResource> FileTree<T> from(
            Project project, Path directory, String pattern, Class<T> type) {
        return (FileTree<T>) ResourceFactory.create(
            ResourceType.create(FileTree.class, type), project,
            project != null ? project.directory().resolve(directory)
                : directory,
            pattern);
    }
}
