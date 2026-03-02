/*
 * JDrupes Builder
 * Copyright (C) 2025, 2026 Michael N. Lipp
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/// [Project]s are used to structure the build configuration. Every build
/// configuration has a single root project and may contain sub-projects.
/// The root project serves as the entry point for the build. Resources
/// provided by the builder are typically provided by the root project,
/// which acts as the central access point of the build configuration.
///
/// Projects are [ResourceProvider]s that obtain resources from related
/// [ResourceProvider]s. Conceptually, a project acts as a router for
/// requests and resources, with its behavior depending on the intended
/// usage of the resources obtained from the providers registered as
/// dependencies. The intended usage is indicated by the [Intent] that
/// attributes the relationship between a project and its related
/// resource providers.
///
/// ## Attributing relationships to providers
///
/// ### Intent Supply
///
/// ![Intent Supply](supply-demo.svg)
///
/// Resources from a provider added with [Intent#Supply] are made available
/// by the project to entities that depend on it. [Intent#Supply] implies
/// that the resources are generated specifically for the project,
/// typically by a [Generator] that belongs to the project.
///
/// ### Intent Consume and Reveal
///
/// ![Intent Consume](consume-demo.svg)
///
/// Resources from a provider added with [Intent#Consume] or
/// [Intent#Reveal] are typically used only by a project's generators.
/// If a provider is added with [Intent#Reveal], its resources are also
/// provided by the project when they are explicitly included in a
/// request.
///
/// ### Intent Expose
///
/// ![Intent Expose](expose-demo.svg)
///
/// Resources from a provider added with [Intent#Expose] (typically
/// another project) are used by a project's generators and are also
/// provided by the project to entities that depend on it.
///
/// ### Intent Forward
///
/// ![Intent Forward](forward-demo.svg)
///
/// Resources from a provider added with [Intent#Forward] (typically
/// another project) are provided by the project to entities that depend
/// on it. These resources are not intended to be used by the project's
/// generators. This cannot be enforced, however, as generators may still
/// access them via [Project#providers()].
///
/// ## Behavior as resource provider
///
/// In its role as a [ResourceProvider], a [Project]
/// [provides resources][ResourceProvider#resources] obtained from its
/// dependencies. A [ResourceRequest] controls to which dependencies a
/// request is forwarded by including the respective [Intent]s in the
/// set returned by [ResourceRequest#uses].
///
/// Concepts from other build tools, such as Gradle’s dependency
/// configurations, can be mapped to resource requests for classpath
/// elements using sets of [Intent]s as follows:
///
/// <table class="simple-table">
///   <thead>
///     <tr>
///       <th style="white-space: nowrap;">Intent \ Config.</th>
///       <th>Api</th>
///       <th>Implementation</th>
///       <th>Compile only</th>
///       <th>Runtime only</th>
///     </tr>
///   </thead>
///   <tbody>
///     <tr><td>Supply</td><td>X</td><td>X</td><td></td><td></td></tr>
///     <tr><td>Consume</td><td></td><td></td><td>X</td><td></td></tr>
///     <tr><td>Reveal</td><td></td><td>X</td><td></td><td></td></tr>
///     <tr><td>Expose</td><td>X</td><td>X</td><td></td><td></td></tr>
///     <tr><td>Forward</td><td></td><td></td><td></td><td>X</td></tr>
///   </tbody>
/// </table>
///
/// To ensure consistent results, a project adjusts a request before
/// forwarding it to a dependency of type [Project] (typically a
/// sub-project). If the request uses `Consume` or `Expose`, `Consume`
/// is removed and `Expose` and `Supply` are added. The reason is that,
/// regardless of how a sub-project contributes to another project, the
/// resources it contributes are always those that are part of its API.
///
/// To avoid misuse of [Intent]s, all intents are removed from a request
/// before it is forwarded to a dependency that is not a project.
///
/// ## Factory method for resources
///
/// As a convenience, this interface also defines a shortcut for creating
/// [Resource]s.
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
public interface Project extends ResourceProvider {

    /// The common project properties.
    ///
    @SuppressWarnings("PMD.FieldNamingConventions")
    enum Properties implements PropertyKey {

        /// The Build directory. Created artifacts should be put there.
        /// Defaults to [Path] "build".
        BuildDirectory(Path.of("build")),
        
        /// The Encoding of files in the project.
        Encoding("UTF-8"),
        
        /// The version of the project. Surprisingly, there is no
        /// agreed upon version type for Java (see e.g. 
        /// ["Version Comparison in Java"](https://www.baeldung.com/java-comparing-versions)).
        /// Therefore the version is represented as a string with "0.0.0"
        /// as default.
        Version("0.0.0");

        private final Object defaultValue;

        <T> Properties(T defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T defaultValue() {
            return (T)defaultValue;
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
    Project project(Class<? extends Project> project);

    /// Returns the parent project. The root project has no parent.
    ///
    /// @return the parent project
    ///
    Optional<Project> parentProject();
    
    /// Returns the project's directory.
    ///
    /// @return the path
    ///
    Path directory();

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
    /// are then provided by the project. For "normal" projects, the
    /// generated resources are assumed to be provided to dependents of
    /// the project, so the invocation is shorthand for  
    /// `dependency(Intent.Supply, generator)`.
    ///
    /// For projects that implement [MergedTestProject], generated resources
    /// are usually intended to be used by the project itself only, so
    /// the invocation is short for `dependency(Intent.Consume, generator)`.
    ///
    /// @param generator the provider
    /// @return the project
    ///
    Project generator(Generator generator);

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
    default <T extends Generator> T generator(Function<Project, T> supplier) {
        var provider = supplier.apply(this);
        generator(provider);
        return provider;
    }

    /// Adds a provider that contributes resources to the project with
    /// the given intended usage.
    ///
    /// While this could be used to add a [Generator] to the project
    /// as a provider with [Intent#Supply], it is recommended to use
    /// one of the "generator" methods for better readability.
    ///
    /// @param intent the dependency type
    /// @param provider the provider
    /// @return the project for method chaining
    /// @see generator(Generator)
    /// @see generator(Function)
    ///
    Project dependency(Intent intent, ResourceProvider provider);

    /// Uses the supplier to create a provider, passing this project as 
    /// argument and adds the result as a dependency to this project. This
    /// is a convenience method to add a provider to the project by writing
    /// (in a project's constructor):
    /// 
    /// ```java
    /// dependency(intent, Provider::new);
    /// ```
    /// instead of:
    /// 
    /// ```java
    /// dependency(intent, new Provider(this));
    /// ```
    ///
    /// @param <T> the generic type
    /// @param intent the intent
    /// @param supplier the supplier
    /// @return the project for method chaining
    ///
    default <T extends ResourceProvider> T dependency(Intent intent,
            Function<Project, T> supplier) {
        var provider = supplier.apply(this);
        dependency(intent, provider);
        return provider;
    }
    
    /// Return a provider selection without any restrictions.
    ///
    /// @return the provider selection
    ///
    ProviderSelection providers();
    
    /// Return a provider selection that is restricted to the given intents.
    ///
    /// @param intents the intents
    /// @return the provider selection
    ///
    ProviderSelection providers(Set<Intent> intents);
    
    /// Return a provider selection that is restricted to the given intents.
    ///
    /// @param intent the intent
    /// @param intents the intents
    /// @return the provider selection
    ///
    default ProviderSelection providers(Intent intent, Intent... intents) {
        return providers(EnumSet.of(intent, intents));
    }

    /// Short for `directory().relativize(other)`.
    ///
    /// @param other the other path
    /// @return the relativized path
    ///
    default Path relativize(Path other) {
        return directory().relativize(other);
    }

    /// Sets the given property to the given value.
    /// 
    /// Regrettably, there is no way to enforce at compile time that the
    /// type of the value passed to `set` matches the type of the property.
    /// An implementation must check this at runtime by verifying that the
    /// given value is assignable to the default value. 
    ///
    /// @param property the property
    /// @param value the value
    /// @return the project
    ///
    Project set(PropertyKey property, Object value);
    
    /// Returns value of the given property of the project. If the
    /// property is not set, the parent project's value is returned.
    /// If neither is set, the property's default value is returned.
    ///
    /// @param <T> the property type
    /// @param property the property
    /// @return the property
    ///
    <T> T get(PropertyKey property);
   
    /// Convenience method for reading the content of a file into a
    /// [String]. The path is resolved against the project's directory.
    ///
    /// @param path the path
    /// @return the string
    ///
    default String readString(Path path) {
        try {
            return Files.readString(directory().resolve(path));
        } catch (IOException e) {
            throw new BuildException().from(this).cause(e);
        }
    }

    
}