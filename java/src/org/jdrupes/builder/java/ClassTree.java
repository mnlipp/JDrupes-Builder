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
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceFactory;
import static org.jdrupes.builder.java.JavaTypes.ClassTreeType;

/// A [FileTree] that consists of [ClassFile]s.
///
public interface ClassTree extends FileTree<ClassFile>, ClasspathElement {

    /// Creates a new class tree from the given values.
    ///
    /// @param project the project
    /// @param directory the directory
    /// @return the class tree
    ///
    static ClassTree from(Project project, Path directory) {
        return ResourceFactory.create(ClassTreeType, project, directory);
    }

}
