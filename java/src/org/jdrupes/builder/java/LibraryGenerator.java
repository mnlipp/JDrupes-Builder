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
import java.util.jar.Attributes;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.IOResource;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceRetriever;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.StreamCollector;
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
/// 2. An [AppJarFile]. When requesting this special jar type, the
///    generator checks if a main class is specified.
///
/// Instead of explicitly adding resources, this generator also supports
/// resource retrieval from added providers. The providers will be used
/// to retrieve resources of type [ClassTree] and [JavaResourceTree] in
/// addition to the explicitly added resources.
///
/// The standard pattern for creating a library is simply:
/// ```java
/// generator(LibraryGenerator::new).from(providers(Supply));
/// ```
///
public class LibraryGenerator extends JarGenerator
        implements ResourceRetriever {

    private final StreamCollector<ResourceProvider> providers
        = StreamCollector.cached();
    private String mainClass;

    /// Instantiates a new library generator.
    ///
    /// @param project the project
    ///
    public LibraryGenerator(Project project) {
        super(project, LibraryJarFileType);
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
    public LibraryGenerator mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /// Additionally uses the given providers for obtaining contents for the
    /// jar.
    ///
    /// @param providers the providers
    /// @return the jar generator
    ///
    @Override
    public LibraryGenerator from(ResourceProvider... providers) {
        from(Stream.of(providers));
        return this;
    }

    /// Additionally uses the given providers for obtaining contents for the
    /// jar.
    ///
    /// @param providers the providers
    /// @return the jar generator
    ///
    @Override
    public LibraryGenerator from(Stream<ResourceProvider> providers) {
        this.providers.add(providers.filter(p -> !p.equals(this)));
        return this;
    }

    /// return the cached providers.
    ///
    /// @return the cached stream
    ///
    protected StreamCollector<ResourceProvider> providers() {
        return providers;
    }

    @Override
    protected void collectContents(Map<Path, Resources<IOResource>> contents) {
        super.collectContents(contents);
        // Add main class if defined
        if (mainClass() != null) {
            attributes(Map.entry(Attributes.Name.MAIN_CLASS, mainClass()));
        }
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
            .parallel().forEach(t -> collect(contents, t));
        project().getFrom(providers().stream(),
            new ResourceRequest<JavaResourceTree>(new ResourceType<>() {}))
            .parallel().forEach(t -> collect(contents, t));
    }

    @Override
    @SuppressWarnings({ "PMD.CollapsibleIfStatements", "unchecked" })
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.includes(LibraryJarFileType)
            && !requested.includes(CleanlinessType)) {
            return Stream.empty();
        }

        // Make sure mainClass is set for app jar
        if (AppJarFileType.isAssignableFrom(requested.type().containedType())
            && mainClass() == null) {
            throw new BuildException("Main class must be set for "
                + name() + " in " + project());
        }

        // Prepare jar file
        var destDir = destination();
        if (!destDir.toFile().exists()) {
            if (!destDir.toFile().mkdirs()) {
                throw new BuildException("Cannot create directory " + destDir);
            }
        }
        var jarResource
            = AppJarFileType.isAssignableFrom(requested.type().containedType())
                ? project().newResource(AppJarFileType,
                    destDir.resolve(jarName()))
                : project().newResource(LibraryJarFileType,
                    destDir.resolve(jarName()));

        // Maybe only delete
        if (requested.includes(CleanlinessType)) {
            jarResource.delete();
            return Stream.empty();
        }

        buildJar(jarResource);
        return Stream.of((T) jarResource);
    }
}
