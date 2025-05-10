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

/// Defines the API for the builder's model of a project.
///
/// Projects form a hierarchy with a single root.
///
public interface Project extends Provider {

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

    /// Returns the directory where the project's tasks should create
    /// the artifacts.
    ///
    /// @return the path
    ///
    Path buildDirectory();

    /// Adds a provider to the project.
    ///
    /// @param provider the provider
    /// @return the project
    ///
    Project provider(Provider<?> provider);

    /// Sets the providers associated with the project. Clears all
    /// already existing providers.
    ///
    /// @param providers the providers
    /// @return the project
    ///
    Project providers(List<Provider<?>> providers);

    /// Adds a provider as a dependency. Resources provided by dependencies
    /// can be used by tasks as input in addition to resources provided
    /// to tasks directly.
    ///
    /// @param provider the provider
    /// @return the project
    ///
    Project dependency(Provider<?> provider);

    /// Sets the dependencies of the project. Clears all already existing
    /// dependencies.
    ///
    /// @param providers the providers
    /// @return the project
    ///
    Project dependencies(List<Provider<?>> providers);

    /// Returns the resources provided to the project by its dependencies.
    ///
    /// @param resource the resource
    /// @return the resources<? extends resource>
    ///
    Resources<? extends Resource> provided(Resource resource);

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
}