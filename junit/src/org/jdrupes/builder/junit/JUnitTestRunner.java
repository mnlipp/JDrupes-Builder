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

package org.jdrupes.builder.junit;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Generator;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceRequest.requestFor;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.TestResult;
import org.jdrupes.builder.core.AbstractGenerator;
import static org.jdrupes.builder.java.JavaTypes.ClasspathType;
import static org.jdrupes.builder.java.JavaTypes.CompilationClasspathType;

/// A [Generator] for [TestResult]s using JUnit.
///
public class JUnitTestRunner extends AbstractGenerator {

    /// Initializes a new test runner.
    ///
    /// @param project the project
    ///
    public JUnitTestRunner(Project project) {
        super(project);
    }

    @Override
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.collects(new ResourceType<TestResult>() {})) {
            return Stream.empty();
        }

        // Collect the classpath. The project under test and the test
        // classes are usually consumed by the test project. Just in case
        // we also include the usual exposed and supplied resources to
        // avoid unexpected behavior.
        var cpResources = newResource(ClasspathType)
            .addAll(project().from(Consume, Expose, Supply)
                .get(requestFor(CompilationClasspathType)));
        log.finest(() -> "Testing in " + project() + " with classpath "
            + cpResources.stream().map(e -> e.toPath().toString())
                .collect(Collectors.joining(File.pathSeparator)));

        // Return result
        return Stream.empty();
    }

}
