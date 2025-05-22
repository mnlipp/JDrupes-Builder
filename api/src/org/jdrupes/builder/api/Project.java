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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/// Defines the API for the builder's model of a project.
///
/// As a convenience, the interface defines factory methods
/// for objects used for defining the project.
///
/// Projects form a hierarchy with a single root.
///
public interface Project extends ResourceProvider<Resource> {

    /// Returns the parent project unless this is the root project.
    ///
    /// @return the (optional) parent project
    ///
    Optional<Project> parent();

    /// Returns the root project.
    ///
    /// @return the project
    ///
    Project rootProject();

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
    /// argument and adds the result as a provider to this project.
    ///
    /// @param supplier the supplier
    /// @return the project
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
    /// @return the project
    ///
    Project providers(List<ResourceProvider<?>> providers);

    /// Adds a provider as a dependency. Resources provided by dependencies
    /// can be used by [Generator]s as input in addition to resources
    /// provided to [Generator]s directly.
    ///
    /// @param provider the provider
    /// @param type the dependency type
    /// @return the project
    ///
    Project dependency(ResourceProvider<?> provider, Dependency.Intend type);

    /// Uses the supplier to create a provider, passing this project as 
    /// argument and adds the result as a dependency to this project.
    ///
    /// This method is typically used to add subproject.
    ///
    /// @param supplier the supplier
    /// @param type the dependency type
    /// @return the project
    ///
    default <T extends ResourceProvider<R>, R extends Resource> T
            dependency(Function<Project, T> supplier, Dependency.Intend type) {
        var dependency = supplier.apply(this);
        dependency(dependency, type);
        return dependency;
    }

    /// Returns the resources provided to the project by its dependencies.
    ///
    /// @param resource the requested resource
    /// @param dependencyTypes the type of dependencies considered
    /// @return the resources<? extends resource>
    ///
    <T extends Resource> Stream<T> provided(Resource resource,
            Set<Dependency.Intend> dependencyTypes);

    /// Short for `directory().relativize(other)`.
    ///
    /// @param other the other path
    /// @return the relativized path
    /// Relativize.
    ///
    /// @param other the other
    /// @return the path
    ///
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
    ///
    FileTree newFileTree(Project project, Path root, String pattern);

    /// Returns a new file tree. The file tree includes all files
    /// matching `pattern` in the tree starting at `root`. `root`
    /// may be specified as absolute path or as path relative to the
    /// `project`'s directory (see [Project#directory]).
    ///
    /// @param project the project
    /// @param root the root of the file tree to search for files matching
    /// `pattern`
    /// @param pattern the pattern
    /// @param kind the file tree's kind
    ///
    FileTree newFileTree(Project project, Path root, String pattern,
            String kind);
}