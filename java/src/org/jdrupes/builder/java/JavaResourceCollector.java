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
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceFile;
import org.jdrupes.builder.core.ResourceCollector;
import static org.jdrupes.builder.java.JavaTypes.*;

/// A [ResourceCollector] for [ResourceFile]s that should be included
/// the resources provided by a Java project.
///
public class JavaResourceCollector
        extends ResourceCollector<JavaResourceTree> {

    /// Instantiates a new Java resources collector.
    ///
    /// @param project the project
    ///
    public JavaResourceCollector(Project project) {
        super(project, JavaResourceTreeType);
    }

    /// Adds the files from the given directory matching the given pattern.
    /// Short for
    /// `add(project().newFileTree(JavaResourceTreeType, directory, pattern))`.
    ///
    /// @param directory the directory
    /// @param pattern the pattern
    /// @return the resources collector
    ///
    public final ResourceCollector<JavaResourceTree> add(Path directory,
            String pattern) {
        add(project().newResource(JavaResourceTreeType, directory, pattern));
        return this;
    }

}
