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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/// Defines the API for a project.
///
/// As a convenience, the interface provides factory methods
/// for objects used for defining the project.
///
/// Projects form a hierarchy with a single root.
///
public interface Project extends ResourceProvider<Resource> {

    /// Returns the root project.
    ///
    /// @return the project
    ///
    Project rootProject();

    /// Returns the instance of the given project class. Projects
    /// are created lazily by the builder and must be accessed
    /// via this method.
    ///
    /// @param project the requested project's type
    /// @return the project
    ///
    Project project(Class<? extends Project> project);

    /// Returns the project's name. 
    ///
    /// @return the string
    ///
    String name();

    /// Returns the project's directory.
    ///
    /// @return the path
    ///
    Path directory();

    /// Returns the directory where the project's [Generator]s should
    /// create the artifacts.
    ///
    /// @return the path
    ///
    Path buildDirectory();

    /// Adds a provider to the project that generates resources which
    /// are then provided by the project. This is short for
    /// `dependency(provider, Intend.Provide)`. 
    ///
    /// @param generator the provider
    /// @return the project
    ///
    Project generator(Generator<?> generator);

    /// Uses the supplier to create a provider, passing this project as 
    /// argument and adds the result as a generator to this project. This
    /// is a convenience method to add a provider to the project by writing
    /// (in a project's constructor):
    /// 
    /// ```java
    /// generator(Provider::new);
    /// ```
    /// instead of:
    /// 
    /// ```java
    /// generator(new Provider(this));
    /// ```
    ///
    /// @param <T> the generic type
    /// @param supplier the supplier
    /// @return the project for method chaining
    ///
    default <T extends Generator<?>> T
            generator(Function<Project, T> supplier) {
        var provider = supplier.apply(this);
        generator(provider);
        return provider;
    }

    /// Returns the providers that have been added with one of the given 
    /// intended usages as [Stream]. The stream may only be terminated
    /// after all projects have been created.
    ///
    /// @return the stream
    ///
    Stream<ResourceProvider<?>> providers(Set<Intend> intends);

    /// Returns the providers that have been added with the given 
    /// intended usage as [Stream]. This is short for
    /// `providers(Set.of(intend))`.
    ///
    /// @return the stream
    ///
    default Stream<ResourceProvider<?>> providers(Intend intend) {
        return providers(Set.of(intend));
    }

    /// Adds a provider that contributes resources to the project with
    /// the given intended usage.
    ///
    /// While this could be used to add a [Generator] to the project
    /// as a provider with [Intend#Provide], it is recommended to use
    /// one of the "generator" methods for better readability.
    ///
    /// @param provider the provider
    /// @param intend the dependency type
    /// @return the project for method chaining
    /// @see #generator(ResourceProvider)
    /// @see #generator(Function)
    ///
    Project dependency(ResourceProvider<?> provider, Intend intend);

    /// Retrieves all providers of the project with the given intended usage,
    /// and returns all resources that are provided by these providers for
    /// the given request.
    ///
    /// @param <T> the requested type
    /// @param intends the type of usage that providers must match
    /// @param requested the requested
    /// @return the provided resources
    ///
    <T extends Resource> Stream<T> provided(Set<Intend> intends,
            ResourceRequest<T> requested);

    /// Short for `directory().relativize(other)`.
    ///
    /// @param other the other path
    /// @return the relativized path
    ///
    default Path relativize(Path other) {
        return directory().relativize(other);
    }

    /// Returns the current build.
    ///
    /// @return the builds the
    ///
    Build build();

    /// Returns a new file tree. The file tree includes all files
    /// matching `pattern` in the tree starting at `root`. `root`
    /// may be specified as absolute path or as path relative to the
    /// `project`'s directory (see [Project#directory]).
    ///
    /// @param project the project
    /// @param root the root of the file tree to search for files matching
    /// `pattern`
    /// @param pattern the pattern
    /// @return the file tree
    ///
    default FileTree<FileResource> newFileTree(Project project, Path root,
            String pattern) {
        return newFileTree(project, root, pattern, FileResource.class);
    }

    /// Returns a new file tree. The file tree includes all files
    /// matching `pattern` in the tree starting at `root`. `root`
    /// may be specified as absolute path or as path relative to the
    /// `project`'s directory (see [Project#directory]).
    ///
    /// @param <T> the type of the [FileResource]s in the tree.
    /// @param project the project
    /// @param root the root of the file tree to search for files matching
    /// `pattern`
    /// @param pattern the pattern
    /// @param type the file tree's type
    /// @return the file tree
    ///
    default <T extends FileResource> FileTree<T> newFileTree(Project project,
            Path root, String pattern, Class<T> type) {
        return newFileTree(project, root, pattern, type, false);
    }

    /// Returns a new file tree. The file tree includes all files
    /// matching `pattern` in the tree starting at `root`. `root`
    /// may be specified as absolute path or as path relative to the
    /// `project`'s directory (see [Project#directory]).
    ///
    /// @param project the project
    /// @param root the root of the file tree to search for files matching
    /// `pattern`
    /// @param pattern the pattern
    /// @param withDirs whether to include directories
    /// @return the file tree
    ///
    default FileTree<FileResource> newFileTree(Project project, Path root,
            String pattern, boolean withDirs) {
        return newFileTree(project, root, pattern, FileResource.class,
            withDirs);
    }

    /// Returns a new file tree. The file tree includes all files
    /// matching `pattern` in the tree starting at `root`. `root`
    /// may be specified as absolute path or as path relative to the
    /// `project`'s directory (see [Project#directory]).
    ///
    /// @param <T> the type of the [FileResource]s in the tree.
    /// @param project the project
    /// @param root the root of the file tree to search for files matching
    /// `pattern`
    /// @param pattern the pattern
    /// @param type the file tree's type
    /// @param withDirs whether to include directories
    /// @return the file tree
    ///
    <T extends FileResource> FileTree<T> newFileTree(Project project, Path root,
            String pattern, Class<T> type, boolean withDirs);

    /// Returns a new file resource.
    ///
    /// @param <T> the type of file resource
    /// @param type the type
    /// @param path the path
    /// @return the file resource
    ///
    <T extends FileResource> T newFileResource(Class<T> type, Path path);

    /// Returns a new resource container.
    ///
    /// @param <T> the contained type
    /// @param type the type of the new object as resource
    /// @return the resources
    ///
    <T extends Resource> Resources<T>
            newResources(Class<? extends Resource> type);

}