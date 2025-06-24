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
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import static org.jdrupes.builder.api.Intend.Consume;
import static org.jdrupes.builder.api.Intend.Expose;
import static org.jdrupes.builder.api.Intend.Forward;
import static org.jdrupes.builder.api.Intend.Supply;

/// [Project]s are used to structure the build configuration. Every
/// build configuration has a single root project and can have
/// sub-projects. The root project is the entry point for the build.
/// The resources provided by the builder are usually provided by the
/// root project that serves as n entry point to the build configuration.
///
/// Projects are [ResourceProvider]s that obtain resources from related
/// [ResourceProvider]s. Projects can be thought of as routers for
/// resources with their behavior depending on the intended usage of the
/// resources from the related providers. The intended usage is specified
/// by the [Intend] that attributes the relationship between a project
/// and its related resource providers.
///
/// ## Attributing relationships to providers
///
/// ### Intend Supply
///
/// ![Intend Supply](doc-files/supply-demo.svg)
///
/// Resources from a provider added with [Intend#Supply] are provided
/// by the project to entities that depend on the project. [Intend#Supply]
/// implies that the resources are genuinely generated for the project
/// (typically by a [Generator] that belongs to the project).
///
/// ### Intend Consume
///
/// ![Intend Consume](doc-files/consume-demo.svg)
///
/// Resources from a provider added with [Intend#Consume] (typically
/// another project) are only available to a project's generators
/// through [Project#provided].   
///
/// ### Intend Expose
///
/// ![Intend Expose](doc-files/expose-demo.svg)
///
/// Resources from a provider added with [Intend#Expose] (typically
/// another project) are provided by the project to entities that
/// depend on the project. They are also available to a project's
/// generators through [Project#provided].
///
/// ### Intend Forward
///
/// ![Intend Forward](doc-files/forward-demo.svg)
///
/// Resources from a provider added with [Intend#Forward] (typically
/// another project) are provided by the project to entities that
/// depend on the project. They are not intended to be used by a
/// project's generators, although these cannot be prevented from
/// accessing them through [Project#provide].
///
/// ## Factory methods
///
/// As a convenience, the interface also defines factory methods
/// for objects used for defining the project.
///
/// @startuml supply-demo.svg
/// object "project: Project" as project
/// object "dependant" as dependant
/// dependant -right-> project
/// object "generator: Generator" as generator
/// project *-down-> generator: "<<Supply>>"
/// @enduml
///
/// @startuml expose-demo.svg
/// object "project: Project" as project
/// object "dependant" as dependant
/// dependant -right-> project
/// object "providing: Project" as providing
/// project *-right-> providing: "<<Expose>>"
/// object "generator: Generator" as generator
/// project *-down-> generator: "   "
/// generator .up.> project: "provided"
/// @enduml
///
/// @startuml consume-demo.svg
/// object "project: Project" as project
/// object "dependant" as dependant
/// dependant -right-> project
/// object "providing: Project" as providing
/// project *-right-> providing: "<<Consume>>"
/// object "generator: Generator" as generator
/// project *-down-> generator: "   "
/// generator .up.> project: "provided"
/// @enduml
///
/// @startuml forward-demo.svg
/// object "project: Project" as project
/// object "dependant" as dependant
/// dependant -right-> project
/// object "providing: Project" as providing
/// project *-right-> providing: "<<Forward>>"
/// object "generator: Generator" as generator
/// project *-down-> generator
/// @enduml
///
@SuppressWarnings("PMD.TooManyMethods")
public interface Project extends ResourceProvider<Resource> {

    /// The common project properties.
    ///
    @SuppressWarnings("PMD.FieldNamingConventions")
    enum Properties implements PropertyKey {

        /// The Build directory. Created artifacts should be put there.
        /// Defaults to [Path] "build".
        BuildDirectory(Path.of("build")),
        
        /// The Encoding of files in the project.
        Encoding("UTF-8");

        private final Object defaultValue;

        <T> Properties(T defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public Object defaultValue() {
            return defaultValue;
        }
    }

    /// Returns the root project.
    ///
    /// @return the project
    ///
    RootProject rootProject();

    /// Returns the instance of the given project class. Projects
    /// are created lazily by the builder and must be accessed
    /// via this method.
    ///
    /// @param project the requested project's type
    /// @return the project
    ///
    ResourceProvider<Resource> project(Class<? extends Project> project);

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

    /// Returns the build context.
    ///
    /// @return the builder configuration
    ///
    BuildContext context();

    /// Returns the directory where the project's [Generator]s should
    /// create the artifacts. This is short for 
    /// `directory().resolve((Path) get(Properties.BuildDirectory))`.
    ///
    /// @return the path
    ///
    default Path buildDirectory() {
        return directory().resolve((Path) get(Properties.BuildDirectory));
    }

    /// Adds a provider to the project that generates resources which
    /// are then provided by the project. This is short for
    /// `dependency(provider, Intend.Provide)`. 
    ///
    /// @param generator the provider
    /// @return the project
    ///
    Project generator(Generator<?> generator);

    /// Uses the supplier to create a provider, passing this project as 
    /// argument and adds the result as a generator to this project. This
    /// is a convenience method to add a provider to the project by writing
    /// (in a project's constructor):
    /// 
    /// ```java
    /// generator(Provider::new);
    /// ```
    /// instead of:
    /// 
    /// ```java
    /// generator(new Provider(this));
    /// ```
    ///
    /// @param <T> the generic type
    /// @param supplier the supplier
    /// @return the project for method chaining
    ///
    default <T extends Generator<?>> T
            generator(Function<Project, T> supplier) {
        var provider = supplier.apply(this);
        generator(provider);
        return provider;
    }

    /// Adds a provider that contributes resources to the project with
    /// the given intended usage.
    ///
    /// While this could be used to add a [Generator] to the project
    /// as a provider with [Intend#Supply], it is recommended to use
    /// one of the "generator" methods for better readability.
    ///
    /// @param intend the dependency type
    /// @param provider the provider
    /// @return the project for method chaining
    /// @see generator(Generator)
    /// @see generator(Function)
    ///
    Project dependency(Intend intend, ResourceProvider<?> provider);

    /// Returns the providers that have been added with one of the given 
    /// intended usages as [Stream]. The stream may only be terminated
    /// after all projects have been created.
    ///
    /// @param intends the intends
    /// @return the stream
    ///
    Stream<ResourceProvider<?>> providers(Set<Intend> intends);

    /// Returns the providers that have been added with the given 
    /// intended usage as [Stream]. This is short for
    /// `providers(Set.of(intend))`.
    ///
    /// @param intend the intend
    /// @param intends more intends
    /// @return the stream
    ///
    default Stream<ResourceProvider<?>> providers(
            Intend intend, Intend... intends) {
        return providers(EnumSet.of(intend, intends));
    }

    /// Invoke the given providers for the given request and return the
    /// resulting stream. This method terminates the stream of providers.
    ///
    /// @param <T> the generic type
    /// @param providers the providers
    /// @param request the request
    /// @return the stream
    ///
    <T extends Resource> Stream<T> invokeProviders(
            Stream<ResourceProvider<?>> providers, ResourceRequest<T> request);

    /// Returns all resources that are provided for the given request
    /// by providers associated with [Intend#Consume] or [Intend#Expose].
    ///
    /// @param <T> the requested type
    /// @param requested the requested
    /// @return the provided resources
    ///
    default <T extends Resource> Stream<T>
            provided(ResourceRequest<T> requested) {
        return invokeProviders(
            providers(EnumSet.of(Consume, Expose)), requested);
    }

    @Override
    default <R extends Resource> Stream<R>
            provide(ResourceRequest<R> requested) {
        return invokeProviders(
            providers(EnumSet.of(Forward, Expose, Supply)), requested);
    }

    /// Short for `directory().relativize(other)`.
    ///
    /// @param other the other path
    /// @return the relativized path
    ///
    default Path relativize(Path other) {
        return directory().relativize(other);
    }

    /// Returns value of the given property of the project. If the
    /// property is not set, the parent project's value is returned.
    /// If neither is set, the property's default value is returned.
    ///
    /// A method for setting a property is is not part of the public API.
    /// It must be provided by the project's implementation as
    /// `protected T set(PropertyKey property, Object value)`,
    /// where `T` is the type of the implementing class.
    ///
    /// Regrettably, there is no way to enforce at compile time that the
    /// type of the value passed to `set` matches the type of the property.
    /// An implementation must check this at runtime.
    ///
    /// @param <T> the generic type
    /// @param property the property
    /// @return the t
    ///
    <T> T get(PropertyKey property);

    /// Obtains the resource stream for the given resource from the
    /// given provider. The result from invoking the provider is
    /// evaluated asynchronously and cached. Only when the returned
    /// stream is terminated will the invocation block until the
    /// result from the provider becomes available.
    /// 
    /// Strictly speaking, this method is not a method of [Project]
    /// as the it does not matter which instance of [Project] is
    /// used to invoke the method. Like the factory methods ("`new...`")
    /// this method is provided to simplify the implementation of a
    /// [Project]'s constructor.
    ///
    /// @param <R> the type of the provided resources
    /// @param provider the provider
    /// @param requested the request
    /// @return the stream of resources
    ///
    <R extends Resource> Stream<R> get(ResourceProvider<?> provider,
            ResourceRequest<R> requested);

    /// Returns a new resource with the given type. Short for invoking
    /// [ResourceFactory#create] with the current project as first argument
    /// and the given arguments appended.
    ///
    /// @param <T> the generic type
    /// @param type the type
    /// @param args the args
    /// @return the t
    ///
    default <T extends Resource> T create(ResourceType<T> type,
            Object... args) {
        return ResourceFactory.create(type, this, args);
    }
}