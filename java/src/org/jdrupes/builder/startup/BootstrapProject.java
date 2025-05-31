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

package org.jdrupes.builder.startup;

import java.nio.file.Path;
import org.jdrupes.builder.api.Dependency.Intend;
import org.jdrupes.builder.api.ResourceFile;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.ResourcesCollector;
import org.jdrupes.builder.java.ClasspathProvider;
import org.jdrupes.builder.java.JavaCompiler;

/// The built-in bootstrap project for compiling the actual (user) project.
///
public class BootstrapProject extends AbstractProject {

    /// Instantiates a new bootstrap project.
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public BootstrapProject() {
        // TODO: support for starting from jar
        directory(Path.of(".jdbld"));
        dependency(
            new ClasspathProvider(this, System.getProperty("java.class.path")),
            Intend.Consume);
        // TODO: configurable pattern
        var bldrDirs = newFileTree(this, Path.of("").toAbsolutePath(),
            "**/_jdbld", true);
        var srcTrees = bldrDirs.stream()
            .map(r -> newFileTree(this, r.path().resolve("src"), "**/*.java"));
        provider(JavaCompiler::new).addSources(srcTrees);
        var resourceTrees = bldrDirs.stream()
            .map(r -> newFileTree(this, r.path().resolve("resources"), "**/*",
                ResourceFile.class));
        provider(ResourcesCollector::new).add(resourceTrees);
    }

}
