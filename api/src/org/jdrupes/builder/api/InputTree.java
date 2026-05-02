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

/// The representation of a tree of [InputResource]s. Each [InputResource]
/// in the tree is identified by a path relative to the root of the tree.
///
/// Implementations of this interface must provide a [ResourceFactory]
/// that supports the invocation of [ResourceFactory#create] with
/// the following arguments
///
/// * The backing [InputResource] for the [InputTree]
/// * [String]\[\] an array of ant-style path patterns
///
/// Implementations of the jdbld API must at least support the creation
/// of an [InputTree] from a [ZipFile].
/// 
/// Implementations of this interface must ensure that the content
/// of the input tree is not evaluated before a consuming operation
/// is performed on the [Stream] returned by [#paths] or [#entries].
///
/// @param <T> the contained type
///
public interface InputTree<T extends InputResource> extends Resources<T> {

    /// An Entry in the tree.
    ///
    /// @param <T> the generic type
    /// @param path the path
    /// @param resource the resource
    ///
    public record Entry<T extends InputResource>(Path path, T resource) {
    }

    /// Add a ant-style path pattern to exclude from the tree.
    ///
    /// @param pattern the pattern
    /// @return the file tree
    ///
    InputTree<T> exclude(String pattern);

    /// Returns the relative paths of the entries in this tree.
    ///
    /// @return the stream
    ///
    Stream<Path> paths();

    /// Returns the paths of the files in this file tree relative to
    /// its root.
    ///
    /// @return the stream
    ///
    Stream<Entry<T>> entries();

    /// Creates a new input file tree from the given backing input resources.
    ///
    /// @param backing the source
    /// @param patterns the patterns. If no patterns are given, the
    /// default pattern "**" is used
    /// @return the file tree
    ///
    @SuppressWarnings({ "PMD.UseDiamondOperator", "PMD.ShortMethodName" })
    static InputTree<InputResource> of(InputResource backing,
            String... patterns) {
        return ResourceFactory.create(
            new ResourceType<InputTree<InputResource>>() {}, null, backing,
            (Object) patterns);
    }
}
