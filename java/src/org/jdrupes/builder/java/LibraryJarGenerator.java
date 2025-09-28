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

package org.jdrupes.builder.java;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.IOResource;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.CachedStream;
import static org.jdrupes.builder.java.JavaTypes.*;

/// A [Generator] for Java libraries packaged as jars. A library jar
/// is expected to contain class files and supporting resources together
/// with additional information in `META-INF/`.
///
/// The generator provides two types of resources.
/// 
/// 1. A [JarFile]. This type of resource is also returned if a more
///    general [ResourceType] such as [ClasspathElement] is requested.
///
/// 2. The sources for the library.
///
/// As a convenience, adding resources to the library is simplified
/// by methods [#add(ResourceProvider...)] and [#addAll(Stream)].
/// These methods allow the user to add providers that will be
/// used to retrieve resources of type [ClassTree] and [JavaResourceTree]
/// in addition to the explicitly added resources.
///
/// The standard pattern for creating a library is simply:
/// ```java
/// generator(JarGenerator::new).addAll(providers(Supply));
/// ```
///
public class LibraryJarGenerator extends JarGenerator {

    private final CachedStream<ResourceProvider> providers
        = new CachedStream<>();
    private String mainClass;

    /// Instantiates a new library generator.
    ///
    /// @param project the project
    ///
    public LibraryJarGenerator(Project project) {
        super(project);
    }

    /// Returns the main class.
    ///
    /// @return the main class
    ///
    public String mainClass() {
        return mainClass;
    }

    /// Sets the main class.
    ///
    /// @param mainClass the new main class
    /// @return the jar generator for method chaining
    ///
    public LibraryJarGenerator mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /// Adds the given providers, see [addAll].
    ///
    /// @param providers the providers
    /// @return the jar generator
    ///
    public LibraryJarGenerator add(ResourceProvider... providers) {
        addAll(Stream.of(providers));
        return this;
    }

    /// Adds the given providers. Each provider will be asked to provide
    /// resources of type [ClassTree] and [JavaResourceTree] when
    /// [#provide] is invoked. All file trees returned in response
    /// are added to the library jar.
    ///
    /// @param providers the providers
    /// @return the library generator
    ///
    public LibraryJarGenerator addAll(Stream<ResourceProvider> providers) {
        this.providers.add(providers.filter(p -> !p.equals(this)));
        return this;
    }

    /// return the cached providers.
    ///
    /// @return the cached stream
    ///
    protected CachedStream<ResourceProvider> providers() {
        return providers;
    }

    @Override
    protected void collectContents(Map<Path, Resources<IOResource>> contents) {
        super.collectContents(contents);
        collectFromProviders(contents);
    }

    /// Collects the contents from the providers. This implementation
    /// requests [ClassTree]s and [JavaResourceTree]s.
    ///
    /// @param contents the contents
    ///
    protected void
            collectFromProviders(Map<Path, Resources<IOResource>> contents) {
        project().getFrom(providers().stream(),
            new ResourceRequest<ClassTree>(new ResourceType<>() {}))
            .parallel().forEach(t -> addFileTree(contents, t));
        project().getFrom(providers().stream(),
            new ResourceRequest<JavaResourceTree>(new ResourceType<>() {}))
            .parallel().forEach(t -> addFileTree(contents, t));
    }

    @Override
    @SuppressWarnings({ "PMD.CollapsibleIfStatements", "unchecked" })
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.includes(JarFileType)
            && !requested.includes(CleanlinessType)) {
            return Stream.empty();
        }

        // Make sure mainClass is set for app jar
        if (AppJarFileType.isAssignableFrom(requested.type().containedType())
            && mainClass == null) {
            throw new BuildException("Main class must be set for "
                + name() + " in " + project());
        }

        // Add main class if defined
        if (mainClass != null) {
            attributes(
                Map.of(Attributes.Name.MAIN_CLASS, mainClass()).entrySet()
                    .stream());
        }

        // Prepare jar file
        var destDir = Optional.ofNullable(destination())
            .orElseGet(() -> project().buildDirectory().resolve("libs"));
        if (!destDir.toFile().exists()) {
            if (!destDir.toFile().mkdirs()) {
                throw new BuildException("Cannot create directory " + destDir);
            }
        }

        // Maybe only delete
        if (requested.includes(CleanlinessType)) {
            project().newResource(JarFileType,
                destDir.resolve(jarName())).delete();
            return Stream.empty();
        }

        var jarResource = project().newResource(JarFileType,
            destDir.resolve(jarName()));
        buildJar(jarResource);
        return Stream.of((T) jarResource);
    }
}
