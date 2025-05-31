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
import java.util.List;
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

    /// Adds a provider to the project.
    ///
    /// @param provider the provider
    /// @return the project
    ///
    Project provider(ResourceProvider<?> provider);

    /// Uses the supplier to create a provider, passing this project as 
    /// argument and adds the result as a provider to this project. This
    /// is a convenience method to add a provider to the project by writing
    /// (in a project's constructor):
    ///
    /// ```java
    /// provider(Provider::new);
    /// ```
    /// instead of:
    ///
    /// ```java
    /// provider(new Provider(this));
    /// ```
    ///
    /// @param <T> the generic type
    /// @param <R> the generic type
    /// @param supplier the supplier
    /// @return the project for method chaining
    ///
    default <T extends ResourceProvider<R>, R extends Resource> T
            provider(Function<Project, T> supplier) {
        var provider = supplier.apply(this);
        provider(provider);
        return provider;
    }

    /// Sets the providers associated with the project. Clears all
    /// already existing providers.
    ///
    /// @param providers the providers
    /// @return the project for method chaining
    ///
    Project providers(List<ResourceProvider<?>> providers);

    /// Adds a provider as a dependency. Resources provided by dependencies
    /// can be used by [Generator]s as input in addition to resources
    /// provided to [Generator]s directly.
    ///
    /// @param provider the provider
    /// @param type the dependency type
    /// @return the project for method chaining
    ///
    Project dependency(ResourceProvider<?> provider, Dependency.Intend type);

    /// Returns the resources provided to the project by its dependencies.
    ///
    /// @param <T> the requested type
    /// @param dependencyTypes the type of dependencies considered
    /// @param requested the requested
    /// @return the resources<? extends resource>
    ///
    <T extends Resource> Stream<T> provided(
            Set<Dependency.Intend> dependencyTypes,
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