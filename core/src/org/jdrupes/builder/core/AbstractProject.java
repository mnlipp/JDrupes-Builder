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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators.AbstractSpliterator;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Intend;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.api.NamedParameter;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.PropertyKey;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.CleanlinessType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.LauncherSupport.CommandData;

/// A default implementation of a [Project].
///
@SuppressWarnings({ "PMD.CouplingBetweenObjects", "PMD.GodClass",
    "PMD.TooManyMethods" })
public abstract class AbstractProject extends AbstractProvider
        implements Project {

    private Map<Class<? extends Project>, Future<Project>> projects;
    private static ThreadLocal<AbstractProject> fallbackParent
        = new ThreadLocal<>();
    private static Path jdbldDirectory = Path.of("marker:jdbldDirectory");
    private final AbstractProject parent;
    private final String projectName;
    private final Path projectDirectory;
    private final Map<ResourceProvider, Intend> providers
        = new ConcurrentHashMap<>();
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<PropertyKey, Object> properties = new HashMap<>();
    // Only non null in the root project
    private DefaultBuildContext context;
    private Map<String, CommandData> commands;

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
    /// cases, the constructor adds a [Intend#Forward] dependency between the
    /// parent project and the new project. This can then be overridden in the
    /// sub project's constructor.
    ///
    /// @param params the named parameters
    ///   * parent - the class of the parent project
    ///   * name - the name of the project. If not provided the name is
    ///     set to the (simple) class name
    ///   * directory - the directory of the project. If not provided,
    ///     the directory is set to the name with uppercase letters
    ///     converted to lowercase for subprojects.
    /// 
    ///     If a project implements [MergedTestProject] and does not 
    ///     specify a directory, its directory is set to the parent
    ///     project's directory.
    /// 
    ///     For root projects the directory is always set to the current
    ///     working directory.
    ///
    @SuppressWarnings({ "PMD.ConstructorCallsOverridableMethod",
        "PMD.UseLocaleWithCaseConversions", "PMD.AvoidCatchingGenericException",
        "PMD.CognitiveComplexity", "PMD.AvoidDeeplyNestedIfStmts" })
    protected AbstractProject(NamedParameter<?>... params) {
        // Evaluate parent project
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
                commands = new HashMap<>(Map.of(
                    "clean", new CommandData("**", new ResourceRequest<?>[] {
                        new ResourceRequest<Cleanliness>(
                            new ResourceType<>() {}) })));
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
            projectDirectory = LauncherSupport.buildRoot();
        } else {
            if (directory == null) {
                if (this instanceof MergedTestProject
                    && parentProject != null) {
                    directory = parent.directory();
                } else {
                    directory = Path.of(projectName.toLowerCase());
                }
            }
            projectDirectory = parent.directory().resolve(directory);
            // Fallback, will be replaced when the parent explicitly adds a
            // dependency.
            parent.dependency(Forward, this);
        }
        try {
            rootProject().prepareProject(this);
        } catch (Exception e) {
            throw new BuildException(e);
        }
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

    /// Parent project.
    ///
    /// @return the optional
    ///
    @Override
    public Optional<Project> parentProject() {
        return Optional.ofNullable(parent);
    }

    /// Name.
    ///
    /// @return the string
    ///
    @Override
    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    public String name() {
        return projectName;
    }

    /// Directory.
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
    public Project generator(Generator provider) {
        if (this instanceof MergedTestProject) {
            providers.put(provider, Consume);
        } else {
            providers.put(provider, Supply);
        }
        return this;
    }

    /// Dependency.
    ///
    /// @param intend the intend
    /// @param provider the provider
    /// @return the project
    ///
    @Override
    public Project dependency(Intend intend, ResourceProvider provider) {
        providers.put(provider, intend);
        return this;
    }

    /// Providers.
    ///
    /// @param intends the intends
    /// @return the stream
    ///
    @Override
    public Stream<ResourceProvider> providers(Set<Intend> intends) {
        return providers.entrySet().stream()
            .filter(e -> intends.contains(e.getValue())).map(Entry::getKey);
    }

    /// Context.
    ///
    /// @return the default build context
    ///
    @Override
    public DefaultBuildContext context() {
        return ((AbstractProject) rootProject()).context;
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

    /// Sets the.
    ///
    /// @param property the property
    /// @param value the value
    /// @return the abstract project
    ///
    @Override
    public AbstractProject set(PropertyKey property, Object value) {
        if (!property.type().isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("Value for " + property
                + " must be of type " + property.type());
        }
        properties.put(property, value);
        return this;
    }

    /// A project itself does not provide any resources. Rather, requests
    /// for resources are forwarded to the project's providers with intend
    /// [Intend#Forward], [Intend#Expose] or [Intend#Supply].
    ///
    /// @param <R> the generic type
    /// @param requested the requested
    /// @return the provided resources
    ///
    @Override
    protected <R extends Resource> Stream<R>
            doProvide(ResourceRequest<R> requested) {
        var providers = providers(Forward, Expose, Supply);
        if (CleanlinessType
            .isAssignableFrom(requested.type().containedType())) {
            providers = Stream.concat(providers,
                providers(Consume).filter(p -> !(p instanceof Project)));
        }
        return from(providers).get(requested);
    }

    /// Define command, see [RootProject#commandAlias].
    ///
    /// @param name the name
    /// @param requests the requests
    /// @return the root project
    ///
    public RootProject commandAlias(String name,
            ResourceRequest<?>... requests) {
        if (commands == null) {
            throw new BuildException("Commands can only be defined for"
                + " the root project.");
        }
        commands.put(name, new CommandData("", requests));
        return (RootProject) this;
    }

    /* default */ CommandData lookupCommand(String name) {
        return commands.getOrDefault(name,
            new CommandData("", new ResourceRequest[0]));
    }

    @SuppressWarnings("PMD.CommentRequired")
    private static class ProjectTreeSpliterator
            extends AbstractSpliterator<Project> {

        private Project next;
        @SuppressWarnings("PMD.LooseCoupling")
        private final Stack<Iterator<Project>> stack = new Stack<>();
        private final Set<Project> seen = new HashSet<>();

        /// Initializes a new project tree spliterator.
        ///
        /// @param root the root
        ///
        public ProjectTreeSpliterator(Project root) {
            super(Long.MAX_VALUE, ORDERED | DISTINCT | IMMUTABLE | NONNULL);
            this.next = root;
        }

        private Iterator<Project> children(Project project) {
            return project.providers(EnumSet.allOf(Intend.class))
                .filter(p -> p instanceof Project).map(Project.class::cast)
                .filter(p -> !seen.contains(p))
                .iterator();
        }

        @Override
        public boolean tryAdvance(Consumer<? super Project> action) {
            if (next == null) {
                return false;
            }
            action.accept(next);
            seen.add(next);
            var children = children(next);
            if (children.hasNext()) {
                next = children.next();
                stack.push(children);
                return true;
            }
            while (!stack.isEmpty()) {
                if (stack.peek().hasNext()) {
                    next = stack.peek().next();
                    return true;
                }
                stack.pop();
            }
            next = null;
            return true;
        }
    }

    /// Provide the projects matching the pattern.
    ///
    /// @param pattern the pattern
    /// @return the stream
    /// @see RootProject#projects(String)
    ///
    public Stream<Project> projects(String pattern) {
        final PathMatcher pathMatcher = FileSystems.getDefault()
            .getPathMatcher("glob:" + pattern);
        return StreamSupport.stream(new ProjectTreeSpliterator(this), false)
            .filter(p -> pathMatcher
                .matches(rootProject().directory().relativize(p.directory())));
    }

    /// Hash code.
    ///
    /// @return the int
    ///
    @Override
    public int hashCode() {
        return Objects.hash(projectDirectory, projectName);
    }

    /// Equals.
    ///
    /// @param obj the obj
    /// @return true, if successful
    ///
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractProject other = (AbstractProject) obj;
        return Objects.equals(projectDirectory, other.projectDirectory)
            && Objects.equals(projectName, other.projectName);
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
