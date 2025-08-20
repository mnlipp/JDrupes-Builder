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

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.core.AbstractGenerator;
import static org.jdrupes.builder.java.JavaTypes.*;

/// Provides [FileTree]s with classes from a given classpath.
///
public class ClasspathScanner extends AbstractGenerator {

    private final String path;

    /// Instantiates a new classpath provider. The `path` is a list of
    /// directories or jar files separated by the system's path separator.
    /// Relative paths are resolved against the project's directory.
    ///
    /// @param project the project
    /// @param path the path
    ///
    public ClasspathScanner(Project project, String path) {
        super(project);
        this.path = path;
    }

    /// Provide [FileTree]s with classes from a given classpath if the
    /// requested resource is of type 
    /// [ClasspathElement](javadoc/org/jdrupes/builder/java/ClasspathElement.html).
    ///
    /// @param <T> the requested type
    /// @param requested the requested resources
    /// @return the stream
    ///
    @Override
    public <T extends Resource> Stream<T>
            provide(ResourceRequest<T> requested) {
        // This supports requests for classpath elements only.
        if (!requested.includes(ClasspathElementType)) {
            return Stream.empty();
        }

        // Map special requests ([RuntimeResources], [CompilationResources])
        // to the base request
        if (!ClasspathType.rawType().equals(requested.type().rawType())) {
            return project().from(this)
                .get(requested.widened(ClasspathType.rawType()));
        }

        @SuppressWarnings("unchecked")
        var result = (Stream<T>) Stream.of(path.split(File.pathSeparator))
            .map(Path::of).map(p -> project().directory().resolve(p)).map(p -> {
                if (p.toFile().isDirectory()) {
                    return (ClasspathElement) project().newResource(
                        ClassTreeType, p.toAbsolutePath());
                } else {
                    return (ClasspathElement) project()
                        .newResource(JarFileType, p.toAbsolutePath());
                }
            }).filter(e -> requested.includes(e.type()));
        return result;
    }

}
