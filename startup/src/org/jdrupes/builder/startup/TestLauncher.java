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

package org.jdrupes.builder.startup;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceType.CleanlinessType;
import org.jdrupes.builder.core.ScopedValueContext;

/// An implementation of a [Launcher] that launches a build configuration.
/// It expects that the JDrupes Builder project has already been compiled
/// and its classes are available on the classpath. It can be used to
/// launch a build configuration from a unit test where the context is
/// already is bound.
///
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestLauncher extends BuildProjectLauncher {

    /// Initializes a new test launcher.
    ///
    /// @param classloader the classloader
    /// @param buildRoot the build root
    /// @param args the args
    ///
    @SuppressWarnings("PMD.UseVarargs")
    public TestLauncher(ClassLoader classloader, Path buildRoot,
            String[] args) {
        super(classloader, buildRoot, args);
    }

    @Override
    public <T extends Resource> Stream<T> resources(Stream<Project> projects,
            ResourceRequest<T> request) {
        var snapshot = ScopedValueContext.snapshot();
        @SuppressWarnings("PMD.CloseResource")
        var context = rootProject().context();
        var result = reportBuildException(() -> projects.parallel()
            .map(p -> snapshot.where(scopedBuildContext, context)
                .call(() -> context.resources(p, request)))
            .flatMap(r -> r).toList().stream());
        if (request.isFor(CleanlinessType)) {
            regenerateRootProject();
        }
        return result;
    }
}
