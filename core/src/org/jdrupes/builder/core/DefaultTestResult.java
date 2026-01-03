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

package org.jdrupes.builder.core;

import java.lang.reflect.Proxy;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Proxyable;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.TestResult;

/// Default implementation of a test result.
///
public class DefaultTestResult extends ResourceObject implements TestResult {

    private final Project project;
    private final String name;
    private final long executed;
    private final long failed;

    /// Initializes a new default test result.
    ///
    /// @param project the project
    /// @param name the name
    /// @param executed the executed
    /// @param failed the failed
    ///
    protected DefaultTestResult(Project project, String name, long executed,
            long failed) {
        this.project = project;
        this.name = name;
        this.executed = executed;
        this.failed = failed;
    }

    /// Creates a new test result.
    ///
    /// @param <T> the resource type
    /// @param type the type
    /// @param project the project
    /// @param name the name
    /// @param executed the number of tests executed
    /// @param failed the number of tests failed
    /// @return the object
    ///
    @SuppressWarnings({ "unchecked" })
    public static <T extends TestResult> T createTestResult(
            ResourceType<T> type, Project project, String name, long executed,
            long failed) {
        return (T) Proxy.newProxyInstance(type.rawType().getClassLoader(),
            new Class<?>[] { type.rawType(), Proxyable.class },
            new ForwardingHandler(
                new DefaultTestResult(project, name, executed, failed)));
    }

    @Override
    public Project project() {
        return project;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public long executed() {
        return executed;
    }

    @Override
    public long failed() {
        return failed;
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        String details;
        if (failed() > 0) {
            details = "failed " + failed() + "/" + executed();
        } else {
            details = executed() + " passed";
        }
        return TestResult.class.getSimpleName() + " from " + project().name()
            + " (" + name() + "): " + details;
    }
}
