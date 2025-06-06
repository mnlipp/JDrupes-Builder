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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Intend;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.PropertyKey;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceRequest.Restriction;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.api.RootProject;

/// A default implementation of a [Project].
///
@SuppressWarnings({ "PMD.TooManyMethods", "PMD.CouplingBetweenObjects" })
public abstract class AbstractProject implements Project {

    private Map<Class<? extends Project>, Project> projects;
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static ThreadLocal<Project> fallbackParent = new ThreadLocal<>();
    private final AbstractProject parent;
    private String name;
    private Path directory;
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<ResourceProvider<?>, Intend> providers
        = new LinkedHashMap<>();
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<PropertyKey, Object> properties = new HashMap<>();
    // Only non null in the root project
    private BuilderData build;

    /// Base class constructor for root projects and subprojects that
    /// do not specify a parent. In the latter case, automatically adds a
    /// [Intend#Forward] dependency between the root project and the
    /// new project.
    ///
    protected AbstractProject() {
        parent = (AbstractProject) fallbackParent.get();
        if (this instanceof RootProject) {
            if (parent != null) {
                throw new BuildException("Root project of type "
                    + getClass().getSimpleName() + " cannot be a sub project.");
            }
            // ConcurrentHashMap does not support null values.
            projects = Collections.synchronizedMap(new HashMap<>());
            projects.put(getClass(), this);
        } else {
            ((AbstractProject) rootProject()).projects.put(getClass(), this);
            parent.dependency(this, Forward);
        }
        // Fallback, overridden when the parent explicitly adds a dependency.
        rootProject().prepareProject(this);
    }

    /// Base class constructor for sub projects that reference their parent
    /// project in the constructor. Automatically adds a [Intend#Forward]
    /// dependency between the parent project and the new project.
    ///
    /// @param parent the parent
    ///
    protected AbstractProject(Project parent) {
        this.parent = (AbstractProject) parent;
        projects.put(getClass(), this);
        // Fallback, overridden when the parent explicitly adds a dependency.
        parent.dependency(this, Forward);
        rootProject().prepareProject(this);
    }

    @Override
    public final RootProject rootProject() {
        if (parent == null) {
            return (RootProject) this;
        }
        return parent.rootProject();
    }

    @Override
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    public Project project(Class<? extends Project> prjCls) {
        if (this.getClass().equals(prjCls)) {
            return this;
        }
        if (parent != null) {
            return parent.project(prjCls);
        }

        // "this" is the root project.
        synchronized (projects) {
            return Optional.ofNullable(projects.get(prjCls)).orElseGet(() -> {
                return createProject(prjCls);
            });
        }
    }

    private Project createProject(Class<? extends Project> prjCls) {
        try {
            fallbackParent.set(this);
            @SuppressWarnings("unchecked")
            var prjConstructor
                = (Constructor<Project>) prjCls.getConstructors()[0];
            if (prjConstructor.getParameterCount() == 0) {
                return prjConstructor.newInstance();
            }
            if (!Project.class
                .isAssignableFrom(prjConstructor.getParameterTypes()[0])) {
                throw new IllegalArgumentException("Argument of project's"
                    + " constructor must implement the Project interface");
            }
            @SuppressWarnings("unchecked")
            var result = prjConstructor.newInstance(project(
                (Class<Project>) prjConstructor.getParameterTypes()[0]));
            return result;
        } catch (SecurityException | InstantiationException
                | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        } finally {
            fallbackParent.set(null);
        }
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
    public Project generator(Generator<?> provider) {
        providers.put(provider, Provide);
        return this;
    }

    @Override
    public Stream<ResourceProvider<?>> providers(Set<Intend> intends) {
        return providers.entrySet().stream()
            .filter(e -> intends.contains(e.getValue())).map(Entry::getKey);
    }

    @Override
    public Project dependency(ResourceProvider<?> provider, Intend intend) {
        providers.put(provider, intend);
        return this;
    }

    @Override
    public <T extends Resource> Stream<T> provided(Set<Intend> intends,
            ResourceRequest<T> requested) {
        return providers.entrySet().stream()
            .filter(e -> intends.contains(e.getValue()))
            .map(e -> context().get(e.getKey(), requested))
            // Terminate stream to start all tasks for evaluating the futures
            .toList().stream().flatMap(r -> r).map(r -> (T) r);
    }

    @Override
    public <R extends Resource> Stream<R>
            provide(ResourceRequest<R> requested) {
        return Stream.concat(
            // Resources generated by the project are always included,
            // else the project would not be used as provider.
            providers(Provide).map(p -> context().get(p, requested)),
            // Considered dependencies depend on the type of the resource.
            Stream.of(provided(requested.restriction() == Restriction.Exposed
                ? EnumSet.of(Expose)
                : EnumSet.of(Expose, Consume, Runtime, Forward), requested)))
            // Terminate stream to start all tasks for evaluating the
            // futures
            .toList().stream().flatMap(r -> r);
    }

    private BuilderData context() {
        if (parent != null) {
            return parent.context();
        }
        if (build != null) {
            return build;
        }
        return build = new BuilderData();
    }

    @Override
    public <T extends Resource> Stream<T> get(ResourceProvider<?> provider,
            ResourceRequest<T> requested) {
        return context().get(provider, requested);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(PropertyKey property) {
        return (T) Optional.ofNullable(properties.get(property))
            .orElseGet(() -> {
                if (parent != null) {
                    return parent.get(property);
                }
                return property.defaultValue();
            });
    }

    /// Sets the given property to the given value.
    ///
    /// @param property the property
    /// @param value the value
    /// @return the abstract project for method chaining
    ///
    protected AbstractProject set(PropertyKey property, Object value) {
        if (!property.type().isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("Value for " + property
                + " must be of type " + property.type());
        }
        properties.put(property, value);
        return this;
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
            Path root, String pattern, Class<T> type, boolean withDirs) {
        return new DefaultFileTree<>(this, root, pattern, type, withDirs);
    }

    @Override
    public String toString() {
        return "Project " + name() + " (in "
            + rootProject().directory().relativize(directory()) + ")";
    }

}
