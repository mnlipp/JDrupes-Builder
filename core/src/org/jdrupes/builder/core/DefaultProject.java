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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.Dependency;
import static org.jdrupes.builder.api.Dependency.Intend.*;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;

/// A default implementation of a [Project].
///
public class DefaultProject implements Project {

    private final Project parent;
    private final String name;
    private final Path directory;
    private final List<ResourceProvider<?>> providers = new ArrayList<>();
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<ResourceProvider<?>, Dependency> dependencies
        = new LinkedHashMap<>();
    private Build build;

    /// Instantiates a new default project.
    ///
    /// @param parent the parent
    /// @param name the name
    /// @param directory the directory
    ///
    public DefaultProject(Project parent, String name, Path directory) {
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
            parent.dependency(this, Build);
        }
    }

    /// Instantiates a new default project.
    ///
    /// @param parent the parent
    /// @param name the name
    ///
    public DefaultProject(Project parent, String name) {
        this(parent, name, null);
    }

    /// Instantiates a new default project.
    ///
    /// @param parent the parent
    /// @param directory the directory
    ///
    public DefaultProject(Project parent, Path directory) {
        this(parent, null, directory);
    }

    /// Instantiates a new default project.
    ///
    /// @param parent the parent
    ///
    public DefaultProject(Project parent) {
        this(parent, null, null);
    }

    @Override
    public Optional<Project> parent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public Project rootProject() {
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
    public Project provider(ResourceProvider<?> provider) {
        providers.add(provider);
        return this;
    }

    @Override
    public Project providers(List<ResourceProvider<?>> providers) {
        this.providers.clear();
        this.providers.addAll(providers);
        return this;
    }

    @Override
    public Project dependency(ResourceProvider<?> provider,
            Dependency.Intend type) {
        dependencies.put(provider, new Dependency(provider, type));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Resource> Stream<T> provided(Resource resource,
            Set<Dependency.Intend> types) {
        return dependencies.values().stream()
            .filter(d -> types.contains(d.type()))
            .map(d -> build().provide(d.provider(), resource))
            // Terminate stream to start all tasks for evaluating the futures
            .toList().stream().flatMap(r -> r).map(r -> (T) r);
    }

    @Override
    public Stream<Resource> provide(Resource resource) {
        return Stream.concat(
            dependencies.values().stream()
                .map(d -> build().provide(d.provider(), resource)),
            providers.stream().map(p -> build().provide(p, resource)))
            // Terminate stream to start all tasks for evaluating the futures
            .toList().stream().flatMap(r -> r);
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

    @Override
    public FileTree newFileTree(Project project, Path root, String pattern) {
        return new DefaultFileTree(project, root, pattern);
    }

    @Override
    public FileTree newFileTree(Project project, Path root, String pattern,
            String kind) {
        return new DefaultFileTree(project, root, pattern).kind(kind);
    }

}
