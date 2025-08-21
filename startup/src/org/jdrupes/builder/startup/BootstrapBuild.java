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
import java.util.Arrays;
import java.util.logging.Logger;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Masked;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.ResourceCollector;
import org.jdrupes.builder.java.ClasspathScanner;
import org.jdrupes.builder.java.JavaCompiler;
import static org.jdrupes.builder.java.JavaTypes.*;

/// The JDrupes Builder project for compiling the user's JDrupes Builder
/// project.
///
public class BootstrapBuild extends AbstractProject implements Masked {

    /* default */ static final String BUILD_EXTENSIONS = "buildExtensions";
    /// The log.
    protected final Logger log = Logger.getLogger(getClass().getName());

    /// Instantiates a new bootstrap project.
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public BootstrapBuild() {
        super(parent(BootstrapRoot.class), jdbldDirectory());

        // Add this (the builder) to the class path. (Build extensions
        // will be added by the bootstrap launcher, because we don't have
        // access to the properties file here.)
        var jcp = Path.of(System.getProperty("java.class.path"))
            .toAbsolutePath().toString();
        log.fine(() -> "Using java.class.path " + jcp
            + " for builder project compilation");
        dependency(Consume, new ClasspathScanner(this, jcp));

        // Collect directories with "build configuration", derive source
        // trees and use as java sources.
        var bldrDirs
            = newResource(new ResourceType<FileTree<FileResource>>() {},
                Path.of("").toAbsolutePath(),
                "**/" + context().jdbldDirectory().toString())
                    .withDirectories();
        addExcludes(bldrDirs);
        var srcTrees = bldrDirs.stream()
            .map(r -> newResource(JavaSourceTreeType, r.path().resolve("src"),
                "**/*.java"));
        generator(JavaCompiler::new).addSources(srcTrees);

        // Add resources
        var resourceTrees = bldrDirs.stream()
            .map(r -> newResource(JavaResourceTreeType,
                r.path().resolve("resources"), "**/*"));
        generator(new ResourceCollector<>(this, JavaResourceTreeType)
            .add(resourceTrees));
    }

    private void addExcludes(FileTree<FileResource> bldrDirs) {
        var itr = Arrays.asList(context().commandArgs()).iterator();
        while (itr.hasNext()) {
            var arg = itr.next();
            if ("-x".equals(arg) && itr.hasNext()) {
                bldrDirs.exclude(itr.next());
            }
        }
    }

}
