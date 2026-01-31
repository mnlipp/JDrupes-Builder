/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
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
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceFile;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.DefaultFileTree;

/// The Class JavaResourceTree.
///
public class DefaultJavaResourceTree
        extends DefaultFileTree<ResourceFile> implements JavaResourceTree {

    /// Instantiates a new java resource tree.
    ///
    /// @param type the type
    /// @param project the project
    /// @param root the root
    /// @param pattern the pattern
    ///
    public DefaultJavaResourceTree(
            ResourceType<? extends JavaResourceTree> type, Project project,
            Path root, String pattern) {
        super(type, project, root, pattern);
    }

    @Override
    public Path toPath() {
        return root();
    }

}
