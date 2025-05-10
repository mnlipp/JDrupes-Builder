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

package org.jdrupes.builder.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Provider;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;

/// A default implementation of a [Project].
///
/// @param <T> the type of resource for the project's role as [Provider]
///
public class DefaultProject<T extends Resource> implements Project<T> {

    private final Project<?> parent;
    private final String name;
    private final Path directory;
    private final List<Provider<?>> providers = new ArrayList<>();
    private final List<Provider<?>> dependencies = new ArrayList<>();
    private Build build;

    /// Instantiates a new default project.
    ///
    /// @param parent the parent
    /// @param name the name
    /// @param directory the directory
    ///
    public DefaultProject(Project<?> parent, String name, Path directory) {
        this.parent = parent;
        if (name == null) {
            name = getClass().getSimpleName();
        }
        this.name = name;
        if (directory == null) {
            directory = Paths.get("").toAbsolutePath().getParent().resolve(name)
                .toAbsolutePath();
        }
        this.directory = directory.toAbsolutePath();
        if (!Files.exists(directory)) {
            throw new IllegalStateException(
                "Invalid project path: " + directory);
        }
        if (parent != null) {
            parent.dependency(this);
        }
    }

    /// Instantiates a new default project.
    ///
    /// @param parent the parent
    /// @param name the name
    ///
    public DefaultProject(Project<?> parent, String name) {
        this(parent, name, null);
    }

    /// Instantiates a new default project.
    ///
    /// @param parent the parent
    /// @param directory the directory
    ///
    public DefaultProject(Project<?> parent, Path directory) {
        this(parent, null, directory);
    }

    /// Instantiates a new default project.
    ///
    /// @param parent the parent
    ///
    public DefaultProject(Project<?> parent) {
        this(parent, null, null);
    }

    @Override
    public Optional<Project<?>> parent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public Project<?> rootProject() {
        if (parent == null) {
            return this;
        }
        return parent.rootProject();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Path directory() {
        return directory;
    }

    @Override
    public Path buildDirectory() {
        return directory.resolve("build");
    }

    @Override
    public Project<T> provider(Provider<?> provider) {
        providers.add(provider);
        return this;
    }

    @Override
    public Project<T> providers(List<Provider<?>> providers) {
        this.providers.clear();
        this.providers.addAll(providers);
        return this;
    }

    @Override
    public Project<T> dependency(Provider<?> provider) {
        if (!dependencies.contains(provider)) {
            dependencies.add(provider);
        }
        return this;
    }

    @Override
    public Project<T> dependencies(List<Provider<?>> providers) {
        this.dependencies.clear();
        this.dependencies.addAll(providers);
        return this;
    }

    @Override
    public Resources<?> provided(Resource resource) {
        return dependencies.stream().map(p -> build().provide(p, resource))
            .map(Resources::stream).flatMap(r -> r)
            .collect(Resources.into(ResourceSet::new));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Resources<T> provide(Resource resource) {
        return (Resources<T>) Stream.concat(
            dependencies.stream().map(p -> build().provide(p, resource)),
            providers.stream().map(p -> build().provide(p, resource)))
            .toList().stream()
            .map(Resources::stream).flatMap(r -> r)
            .collect(Resources.into(ResourceSet::new));
    }

    @Override
    public Build build() {
        if (parent != null) {
            return parent.build();
        }
        if (build != null) {
            return build;
        }
        return build = new DefaultBuild();
    }
}
