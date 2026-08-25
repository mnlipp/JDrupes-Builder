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
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.core.AbstractGenerator;
import static org.jdrupes.builder.java.JavaTypes.*;

/// Provides [ClassTree]s or [LibraryJarFile]s from a given classpath.
///
public class ClasspathScanner extends AbstractGenerator {

    private String path;

    /// Instantiates a new classpath generator. The path needs to be
    /// set with [#path].
    ///
    /// @param project the project
    ///
    public ClasspathScanner(Project project) {
        super(project);
        path = "";
    }

    /// Sets the path. The `path` is a list of directories or jar
    /// files separated by the system's path separator. Relative
    /// paths are resolved against the project's directory.
    ///
    /// @param path the path
    /// @return the classpath scanner
    ///
    public ClasspathScanner path(String path) {
        this.path = path;
        return this;
    }

    @Override
    protected <T extends Resource> Collection<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.accepts(CodeContributionType)) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        var result = (Collection<T>) Stream.of(path.split(File.pathSeparator))
            .map(Path::of).map(p -> project().directory().resolve(p)).map(p -> {
                if (p.toFile().isDirectory()) {
                    return ClassTree.of(project(), p.toAbsolutePath());
                } else {
                    return LibraryJarFile.of(p.toAbsolutePath());
                }
            }).filter(e -> requested.accepts(e.type())).toList();
        return result;
    }

}
