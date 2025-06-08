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
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.AbstractProject;
import static org.jdrupes.builder.core.CoreTypes.*;
import org.jdrupes.builder.core.ResourcesCollector;
import org.jdrupes.builder.java.ClasspathScanner;
import org.jdrupes.builder.java.JavaCompiler;
import static org.jdrupes.builder.java.JavaTypes.*;

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
        directory(context().jdbldDirectory());
        dependency(
            new ClasspathScanner(this, System.getProperty("java.class.path")),
            Intend.Consume);

        // Collect directories with "build configuration", derive source
        // trees and use as java sources.
        var bldrDirs = newFileTree(new ResourceType<>() {
        }, Path.of("").toAbsolutePath(),
            "**/" + context().jdbldDirectory().toString(), true);
        var srcTrees = bldrDirs.stream()
            .map(r -> newFileTree(JavaSourceFiles, r.path().resolve("src"),
                "**/*.java"));
        generator(JavaCompiler::new).addSources(srcTrees);

        // Add resources
        var resourceTrees = bldrDirs.stream()
            .map(r -> newFileTree(ResourceFiles, r.path().resolve("resources"),
                "**/*"));
        generator(ResourcesCollector::new).add(resourceTrees);
    }

}
