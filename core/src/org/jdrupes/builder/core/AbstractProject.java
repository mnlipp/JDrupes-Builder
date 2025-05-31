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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Dependency;
import org.jdrupes.builder.api.Dependency.Intend;
import static org.jdrupes.builder.api.Dependency.Intend.*;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceRequest.Restriction;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.api.RootProject;

/// A default implementation of a [Project].
///
@SuppressWarnings("PMD.TooManyMethods")
public abstract class AbstractProject implements Project {

    private Map<Class<? extends Project>, Project> projects;
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final ThreadLocal<
            List<Class<? extends Project>>> detectedSubprojects
                = ThreadLocal.withInitial(Collections::emptyList);
    private static ThreadLocal<Project> projectInstantiator
        = new ThreadLocal<>();
    private Project parent;
    private String name;
    private Path directory;
    private final List<ResourceProvider<?>> providers = new ArrayList<>();
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<ResourceProvider<?>, Dependency> dependencies
        = new LinkedHashMap<>();
    private Build build;

    /* default */
    static void detectedSubprojects(List<Class<? extends Project>> subClasses) {
        detectedSubprojects.set(subClasses);
    }

    /// Base class constructor for sub projects. Automatically adds a
    /// [Build] dependency between the root project and the new project.
    ///
    /// This constructor may be used by root projects if there are no
    /// sub projects or if detection of projects from the classpath is
    /// is used (see [AbstractLauncher#DefaultLauncher()]).
    ///
    @SuppressWarnings({ "unchecked", "PMD.ClassCastExceptionWithToArray" })
    protected AbstractProject() {
        parent = projectInstantiator.get();
        if (this instanceof RootProject) {
            if (parent != null) {
                throw new BuildException("Root project of type "
                    + getClass().getSimpleName() + " cannot be a sub project.");
            }
            initRootProject((Class<? extends Project>[]) detectedSubprojects
                .get().toArray(new Class<?>[0]));
            return;
        }

        // Fallback, overridden when the parent explicitly adds a dependency.
        parent.dependency(this, Build);
    }

    /// Base class constructor for the root project.
    ///
    /// @param subprojects the sub projects
    ///
    @SafeVarargs
    protected AbstractProject(Class<? extends Project>... subprojects) {
        initRootProject(subprojects);
    }

    @SuppressWarnings("PMD.UseVarargs")
    private void initRootProject(Class<? extends Project>[] subprojects) {
        parent = null;
        // ConcurrentHashMap does not support null values.
        projects = Collections.synchronizedMap(new HashMap<>());
        projects.put(getClass(), this);
        for (var sub : subprojects) {
            projects.put(sub, null);
        }
    }

    /* default */ void createProjects() {
        projects.keySet().stream().forEach(this::project);
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
        if (this.getClass().equals(project)) {
            return this;
        }
        if (parent != null) {
            return parent.project(project);
        }
        return projects.compute(project, (k, v) -> {
            if (v != null) {
                return v;
            }
            try {
                projectInstantiator.set(this);
                var newProject = k.getConstructor().newInstance();
                projectInstantiator.set(null);
                return newProject;
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

    /// Sets the project's directory. The path is resolved against the
    /// parent project's directory. If the project is the root project,
    /// the path is resolved against the current working directory.
    ///
    /// @param path the directory
    /// @return the project
    ///
    public AbstractProject directory(Path path) {
        if (parent == null) {
            directory = Paths.get("").toAbsolutePath().resolve(path);
        } else {
            directory = path;
        }
        return this;
    }

    /// Returns the project's directory. If no directory has been set
    /// explicitly, returns the result of resolving the project's name
    /// against the parent project's directory. The root project's
    /// directory defaults to the current working directory.
    ///
    /// @return the path
    ///
    @Override
    public Path directory() {
        if (parent == null) {
            return Optional.ofNullable(directory)
                .orElseGet(() -> Path.of("").toAbsolutePath());
        }
        // Use parent's directory to resolve explicitly set directory
        // or use name as (sub)directory.
        return parent.directory()
            .resolve(Optional.ofNullable(directory).orElse(Path.of(name()))
                .normalize());
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
    public <T extends Resource> Stream<T> provided(Set<Intend> types,
            ResourceRequest<T> requested) {
        return dependencies.values().stream()
            .filter(d -> types.contains(d.intend()))
            .map(d -> build().provide(d.provider(), requested))
            // Terminate stream to start all tasks for evaluating the futures
            .toList().stream().flatMap(r -> r).map(r -> (T) r);
    }

    @Override
    public <R extends Resource> Stream<R>
            provide(ResourceRequest<R> requested) {
        return Stream.concat(
            // Resources generated by project are always included,
            // else the project would not be used as provider.
            providers.stream().map(p -> build().provide(p, requested)),
            // Considered dependencies depend on the type of the resource.
            Stream.of(provided(requested.restriction() == Restriction.Exposed
                ? EnumSet.of(Expose)
                : EnumSet.of(Expose, Consume, Runtime, Build), requested)))
            // Terminate stream to start all tasks for evaluating the
            // futures
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
    public <T extends FileResource> T newFileResource(Class<T> type,
            Path path) {
        return DefaultFileResource.create(type, path);
    }

    @Override
    public <T extends Resource> Resources<T>
            newResources(Class<? extends Resource> type) {
        return new DefaultResources<>(type);
    }

    @Override
    public <T extends FileResource> FileTree<T> newFileTree(
            Project project, Path root, String pattern, Class<T> type,
            boolean withDirs) {
        return new DefaultFileTree<>(project, root, pattern, type, withDirs);
    }

    @Override
    public String toString() {
        return "Project " + name() + " (in "
            + rootProject().directory().relativize(directory()) + ")";
    }

}
