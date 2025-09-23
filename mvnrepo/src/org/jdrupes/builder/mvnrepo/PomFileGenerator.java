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

package org.jdrupes.builder.mvnrepo;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Generator;
import static org.jdrupes.builder.api.Intend.Supply;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.core.AbstractGenerator;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

/// A [Generator] for POM files.
///
/// The generator provides a single type of resource.
/// 
///
public class PomFileGenerator extends AbstractGenerator {

    private Path destination = Path.of("maven");

    /// Instantiates a new library generator.
    ///
    /// @param project the project
    ///
    public PomFileGenerator(Project project) {
        super(project);
    }

    /// Returns the destination directory. Defaults to "`maven`".
    ///
    /// @return the destination
    ///
    public Path destination() {
        return destination;
    }

    /// Sets the destination directory. The [Path] is resolved against
    /// the project's build directory (see [Project#buildDirectory]).
    ///
    /// @param destination the new destination
    /// @return the java compiler
    ///
    public PomFileGenerator destination(Path destination) {
        this.destination = destination;
        return this;
    }

    @Override
    public <T extends Resource> Stream<T>
            provide(ResourceRequest<T> requested) {
        var pomPath = project().buildDirectory().resolve(destination)
            .resolve("pom.xml");

        // Maybe only delete
        if (requested.includes(Cleaniness)) {
            if (pomPath.toFile().exists()) {
                pomPath.toFile().delete();
            }
            return Stream.empty();
        }

        if (!requested.includes(PomFileType)) {
            return Stream.empty();
        }

        var deps = project().newResource(MvnRepoDependenciesType).addAll(
            project().provided(new ResourceRequest<>(
                MvnRepoDependenciesType).forwardTo(EnumSet.of(Supply))));
        return Stream.empty();
//        log.finest(() -> "Compiling in " + project() + " with classpath "
//            + cpResources.stream().map(e -> e.toPath().toString())
//                .collect(Collectors.joining(File.pathSeparator)));
    }
}
