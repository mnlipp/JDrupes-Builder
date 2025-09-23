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

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import static org.jdrupes.builder.java.JavaTypes.*;

/// A [Generator] for Java libraries (jar files).
///
/// The generator provides two types of resources.
/// 
/// 1. A [JarFile]. This type of resource is also returned if a more
///    general [ResourceType] such as [ClasspathElement] is requested.
///
/// 2. The sources for the library.
///
/// The standard pattern for creating a library is simply:
/// ```java
/// generator(LibraryGenerator::new).addAll(providers(Supply));
/// ```
///
public class LibraryGenerator extends JarGenerator {

    /// Instantiates a new library generator.
    ///
    /// @param project the project
    ///
    public LibraryGenerator(Project project) {
        super(project);
    }

    @Override
    @SuppressWarnings({ "PMD.CollapsibleIfStatements", "unchecked" })
    public <T extends Resource> Stream<T>
            provide(ResourceRequest<T> requested) {
        if (!requested.includes(JarFileType)
            && !requested.includes(Cleaniness)) {
            return Stream.empty();
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
        if (requested.includes(Cleaniness)) {
            project().newResource(JarFileType,
                destDir.resolve(jarName())).delete();
            return Stream.empty();
        }

        // Get all content.
        log.fine(() -> "Getting library jar content for " + jarName());
        var toBeIncluded = project().newResource(ClasspathType)
            .addAll(project().invokeProviders(providers().stream(),
                new ResourceRequest<ClassTree>(new ResourceType<>() {})))
            .addAll(project().invokeProviders(providers().stream(),
                new ResourceRequest<JavaResourceTree>(
                    new ResourceType<>() {})));
        log.fine(() -> "Library jar content: " + toBeIncluded.stream()
            .map(e -> project().relativize(e.toPath()).toString())
            .collect(Collectors.joining(":")));

        // Check if rebuild needed.
        var jarResource = project().newResource(JarFileType,
            destDir.resolve(jarName()));
        if (jarResource.asOf().isAfter(toBeIncluded.asOf())) {
            return Stream.of((T) jarResource);
        }
        buildJar(jarResource, toBeIncluded);
        return Stream.of((T) jarResource);
    }
}
