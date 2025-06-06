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
import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.api.Masked;
import org.jdrupes.builder.api.ResourceFile;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.ResourcesCollector;
import org.jdrupes.builder.java.ClasspathScanner;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaSourceFile;

/// The JDrupes Builder project for compiling the user's JDrupes Builder
/// project.
///
public class BootstrapBuild extends AbstractProject implements Masked {

    /// Instantiates a new bootstrap project.
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public BootstrapBuild() {
        super(BootstrapRoot.class);
        // TODO: support for starting from jar
        directory(Path.of(".jdbld"));
        dependency(
            new ClasspathScanner(this, System.getProperty("java.class.path")),
            Intend.Consume);
        // TODO: configurable pattern
        var bldrDirs = newFileTree(Path.of("").toAbsolutePath(), "**/_jdbld",
            true);
        var srcTrees = bldrDirs.stream()
            .map(r -> newFileTree(r.path().resolve("src"), "**/*.java",
                JavaSourceFile.class));
        generator(JavaCompiler::new).addSources(srcTrees);
        var resourceTrees = bldrDirs.stream()
            .map(r -> newFileTree(r.path().resolve("resources"), "**/*",
                ResourceFile.class));
        generator(ResourcesCollector::new).add(resourceTrees);
    }

}
