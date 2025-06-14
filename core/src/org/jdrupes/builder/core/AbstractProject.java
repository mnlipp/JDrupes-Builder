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
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Intend;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.NamedParameter;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.PropertyKey;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceRequest.Restriction;
import org.jdrupes.builder.api.RootProject;

/// A default implementation of a [Project].
///
@SuppressWarnings({ "PMD.TooManyMethods", "PMD.CouplingBetweenObjects",
    "PMD.GodClass" })
public abstract class AbstractProject implements Project {

    private Map<Class<? extends Project>, Future<Project>> projects;
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static ThreadLocal<AbstractProject> fallbackParent
        = new ThreadLocal<>();
    private static Path jdbldDirectory = Path.of("marker:jdbldDirectory");
    private final AbstractProject parent;
    private final String projectName;
    private final Path projectDirectory;
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<ResourceProvider<?>, Intend> providers
        = new LinkedHashMap<>();
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<PropertyKey, Object> properties = new HashMap<>();
    // Only non null in the root project
    private DefaultBuildContext context;

    /// Named parameter for specifying the parent project.
    ///
    /// @param parentProject the parent project
    /// @return the named parameter
    ///
    protected static NamedParameter<Class<? extends Project>>
            parent(Class<? extends Project> parentProject) {
        return new NamedParameter<>("parent", parentProject);
    }

    /// Named parameter for specifying the name.
    ///
    /// @param name the name
    /// @return the named parameter
    ///
    protected static NamedParameter<String> name(String name) {
        return new NamedParameter<>("name", name);
    }

    /// Named parameter for specifying the directory.
    ///
    /// @param directory the directory
    /// @return the named parameter
    ///
    protected static NamedParameter<Path> directory(Path directory) {
        return new NamedParameter<>("directory", directory);
    }

    /// Hack to pass `context().jdbldDirectory()` as named parameter
    /// for the directory to the constructor. This is required because
    /// you cannot "refer to an instance method while explicitly invoking
    /// a constructor". 
    ///
    /// @return the named parameter
    ///
    protected static NamedParameter<Path> jdbldDirectory() {
        return new NamedParameter<>("directory", jdbldDirectory);
    }

    /// Base class constructor for all projects. The behavior depends 
    /// on whether the project is a root project (implements [RootProject])
    /// or a subproject and on whether the project specifies a parent project.
    ///
    /// [RootProject]s must invoke this constructor with a null parent project
    /// class.
    ///
    /// A sub project that wants to specify a parent project must invoke this
    /// constructor with the parent project's class. If a sub project does not
    /// specify a parent project, the root project is used as parent. In both
    /// cases, the constructor adds a [Intend#Ignore] dependency between the
    /// parent project and the new project. This can then be overridden in the
    /// sub project's constructor.
    ///
    /// @param params the named parameters
    ///   * parent - the class of the parent project
    ///   * name - the name of the project. If not provided the name is
    ///     set to the (simple) class name
    ///   * directory - the directory of the project. If not provided,
    ///     the directory is set to the name with uppercase letters
    ///     converted to lowercase for subprojects. For root projects
    ///     the directory is always set to the current working
    ///
    @SuppressWarnings({ "PMD.ConstructorCallsOverridableMethod",
        "PMD.UseLocaleWithCaseConversions", "PMD.UnusedFormalParameter" })
    protected AbstractProject(NamedParameter<?>... params) {
        // Evaluate patent project
        var parentProject = NamedParameter.<
                Class<? extends Project>> get(params, "parent", null);
        if (parentProject == null) {
            parent = fallbackParent.get();
            if (this instanceof RootProject) {
                if (parent != null) {
                    throw new BuildException("Root project of type "
                        + getClass().getSimpleName()
                        + " cannot be a sub project.");
                }
                // ConcurrentHashMap does not support null values.
                projects = Collections.synchronizedMap(new HashMap<>());
                context = new DefaultBuildContext();
            }
        } else {
            parent = (AbstractProject) project(parentProject);
        }

        // Set name and directory, add fallback dependency
        var name = NamedParameter.<String> get(params, "name",
            () -> getClass().getSimpleName());
        projectName = name;
        var directory = NamedParameter.<Path> get(params, "directory", null);
        if (directory == jdbldDirectory) { // NOPMD
            directory = context().jdbldDirectory();
        }
        if (parent == null) {
            if (directory != null) {
                throw new BuildException("Root project of type "
                    + getClass().getSimpleName()
                    + " cannot specify a directory.");
            }
            projectDirectory = Path.of("").toAbsolutePath();
        } else {
            if (directory == null) {
                directory = Path.of(projectName.toLowerCase());
            }
            projectDirectory = parent.directory().resolve(directory);
            // Fallback, will be replaced when the parent explicitly adds a
            // dependency.
            parent.dependency(this, Ignore);
        }
        rootProject().prepareProject(this);
    }

    /// Root project.
    ///
    /// @return the root project
    ///
    @Override
    public final RootProject rootProject() {
        if (this instanceof RootProject root) {
            return root;
        }
        // The method may be called (indirectly) from the constructor
        // of a subproject, that specifies its parent project class, to
        // get the parent project instance. In this case, the new
        // project's parent attribute has not been set yet and we have
        // to use the fallback.
        return Optional.ofNullable(parent).orElse(fallbackParent.get())
            .rootProject();
    }

    /// Project.
    ///
    /// @param prjCls the prj cls
    /// @return the project
    ///
    @Override
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    public Project project(Class<? extends Project> prjCls) {
        if (this.getClass().equals(prjCls)) {
            return this;
        }
        if (projects == null) {
            return rootProject().project(prjCls);
        }

        // "this" is the root project.
        try {
            return projects.computeIfAbsent(prjCls, k -> {
                return context().executor().submit(() -> {
                    try {
                        fallbackParent.set(this);
                        return (Project) k.getConstructor().newInstance();
                    } catch (SecurityException | InstantiationException
                            | IllegalAccessException
                            | InvocationTargetException
                            | NoSuchMethodException e) {
                        throw new IllegalArgumentException(e);
                    } finally {
                        fallbackParent.set(null);
                    }
                });
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /// Returns the project's name.
    ///
    /// @return the string
    ///
    @Override
    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    public String name() {
        return projectName;
    }

    /// Returns the project's directory.
    ///
    /// @return the path
    ///
    @Override
    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    public Path directory() {
        return projectDirectory;
    }

    /// Generator.
    ///
    /// @param provider the provider
    /// @return the project
    ///
    @Override
    public Project generator(Generator<?> provider) {
        providers.put(provider, Supply);
        return this;
    }

    /// Providers.
    ///
    /// @param intends the intends
    /// @return the stream
    ///
    @Override
    public Stream<ResourceProvider<?>> providers(Set<Intend> intends) {
        return providers.entrySet().stream()
            .filter(e -> intends.contains(e.getValue())).map(Entry::getKey);
    }

    /// Dependency.
    ///
    /// @param provider the provider
    /// @param intend the intend
    /// @return the project
    ///
    @Override
    public Project dependency(ResourceProvider<?> provider, Intend intend) {
        providers.put(provider, intend);
        return this;
    }

    /// Provided.
    ///
    /// @param <T> the generic type
    /// @param intends the intends
    /// @param requested the requested
    /// @return the stream
    ///
    @Override
    public <T extends Resource> Stream<T> provided(Set<Intend> intends,
            ResourceRequest<T> requested) {
        return providers.entrySet().stream()
            .filter(e -> intends.contains(e.getValue()))
            .map(e -> context().get(e.getKey(), requested))
            // Terminate stream to start all tasks for evaluating the futures
            .toList().stream().flatMap(r -> r).map(r -> (T) r);
    }

    /// Provide.
    ///
    /// @param <R> the generic type
    /// @param requested the requested
    /// @return the stream
    ///
    @Override
    public <R extends Resource> Stream<R>
            provide(ResourceRequest<R> requested) {
        return Stream.concat(
            // Resources generated by the project are always included,
            // else the project would not be used as provider.
            providers(Supply).map(p -> context().get(p, requested)),
            // Considered dependencies depend on the type of the resource.
            Stream.of(provided(requested.restriction() == Restriction.Exposed
                ? EnumSet.of(Expose)
                : EnumSet.of(Expose, Consume, Forward, Ignore), requested)))
            // Terminate stream to start all tasks for evaluating the
            // futures
            .toList().stream().flatMap(r -> r);
    }

    /// Context.
    ///
    /// @return the default build context
    ///
    @Override
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    public DefaultBuildContext context() {
        return ((AbstractProject) rootProject()).context;
    }

    /// Returns the.
    ///
    /// @param <T> the generic type
    /// @param provider the provider
    /// @param requested the requested
    /// @return the stream
    ///
    @Override
    public <T extends Resource> Stream<T> get(ResourceProvider<?> provider,
            ResourceRequest<T> requested) {
        return context().get(provider, requested);
    }

    /// Returns the.
    ///
    /// @param <T> the generic type
    /// @param property the property
    /// @return the t
    ///
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

    /// Implements [RootProject#execute].
    ///
    /// @param name the name
    ///
    public void execute(String name) {
        try {
            Method method = getClass().getMethod(name);
            method.invoke(this);
        } catch (NoSuchMethodException e) {
            throw new BuildException(this + " does not support " + name, e);
        } catch (SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            throw new BuildException("Problem invoking " + name + " on " + this
                + (e.getMessage() == null ? "" : (": " + e.getMessage())), e);
        }
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        var relDir = rootProject().directory().relativize(directory());
        return "Project " + name() + (relDir.toString().isBlank() ? ""
            : (" (in " + relDir + ")"));
    }

}
