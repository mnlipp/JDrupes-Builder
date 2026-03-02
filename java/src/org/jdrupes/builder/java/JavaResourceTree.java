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

package org.jdrupes.builder.java;

import java.nio.file.Path;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceFile;
import static org.jdrupes.builder.java.JavaTypes.JavaResourceTreeType;

/// A [FileTree] that consists of [ResourceFile]s and can be used in
/// a Java classpath.
///
public interface JavaResourceTree
        extends FileTree<ResourceFile>, ClasspathElement {

    /// Creates a new Java resource tree from the given values.
    ///
    /// @param project the project
    /// @param directory the directory
    /// @param pattern the pattern
    /// @return the java resource tree
    ///
    static JavaResourceTree from(Project project, Path directory,
            String pattern) {
        return ResourceFactory.create(JavaResourceTreeType, project, directory,
            pattern);
    }
}
