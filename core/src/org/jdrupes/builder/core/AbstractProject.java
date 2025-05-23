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

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public abstract class AbstractProject implements Project {

    private Map<Class<? extends Project>, Project> projects;
    private static ThreadLocal<Project> projectInstantiator
        = new ThreadLocal<>();
    private final Project parent;
    private String name;
    private Path directory;
    private final List<ResourceProvider<?>> providers = new ArrayList<>();
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<ResourceProvider<?>, Dependency> dependencies
        = new LinkedHashMap<>();
    private Build build;

    /// Base class constructor for all projects except the root project.
    /// Automatically adds a [Build] dependency between the root project
    /// and the new project.
    ///
    protected AbstractProject() {
        parent = projectInstantiator.get();
        parent.dependency(this, Build);
    }

    /// Base class constructor for the root project.
    ///
    /// @param subprojects the sub projects
    ///
    @SuppressWarnings("PMD.UseVarargs")
    protected AbstractProject(Class<Project>[] subprojects) {
        parent = null;
        // ConcurrentHashMap does not support null values.
        projects = Collections.synchronizedMap(new HashMap<>());
        projects.put(getClass(), this);
        for (var sub : subprojects) {
            projects.put(sub, null);
        }
    }

    @Override
    public Project rootProject() {
        if (parent == null) {
            return this;
        }
        return parent.rootProject();
    }

    @Override
    public Project project(Class<? extends Project> project) {
        if (parent != null) {
            return parent.project(project);
        }
        return projects.compute(project, (k, v) -> {
            if (v != null) {
                return v;
            }
            try {
                projectInstantiator.set(this);
                return k.getConstructor().newInstance();
            } catch (NoSuchMethodException | SecurityException
                    | InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    /// Sets the project's name.
    ///
    /// @param name the name
    /// @return the project
    ///
    public AbstractProject name(String name) {
        this.name = name;
        return this;
    }

    /// Returns the project's name. Returns the simple name of the project's
    /// class, if no name has been set explicitly. 
    ///
    /// @return the string
    ///
    @Override
    public String name() {
        if (name == null) {
            return getClass().getSimpleName();
        }
        return name;
    }

    /// Sets the project's directory.
    ///
    /// @param path the directory
    /// @return the project
    ///
    public AbstractProject directory(Path path) {
        directory = path;
        return this;
    }

    /// Returns the project's directory. Returns the result of resolving
    /// the project's name against the parent project's directory, if
    /// no directory has been set explicitly. 
    ///
    /// @return the path
    ///
    @Override
    public Path directory() {
        if (directory == null) {
            return Paths.get("").toAbsolutePath().getParent().resolve(name())
                .toAbsolutePath();

        }
        return directory;
    }

    @Override
    public Path buildDirectory() {
        return directory().resolve("build");
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
