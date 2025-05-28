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
import org.jdrupes.builder.core.AbstractGenerator;

/// Provides [FileTree]s with classes from a given classpath.
///
public class ClasspathProvider extends AbstractGenerator<FileTree> {

    private final String path;

    /// Instantiates a new classpath provider.
    ///
    /// @param project the project
    /// @param path the path (directories separated by the system's
    /// path separator)
    ///
    public ClasspathProvider(Project project, String path) {
        super(project);
        this.path = path;
    }

    /// Provide [FileTree]s with classes from a given classpath if the
    /// requested resource as kind [Resource.KIND_CLASSES].
    ///
    /// @param requested the requested
    /// @return the stream
    ///
    @Override
    public Stream<FileTree> provide(Resource requested) {
        if (!Resource.KIND_CLASSES.equals(requested.kind())) {
            return Stream.empty();
        }
        return Stream.of(path.split(File.pathSeparator)).map(d -> project()
            .newFileTree(project(), Path.of(d), "**/*.class",
                Resource.KIND_CLASSES));
    }

}
