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
import java.util.Objects;
import org.jdrupes.builder.api.FaultAware;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Proxyable;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.TestResult;

/// Default implementation of a test result.
///
public class DefaultTestResult extends ResourceObject implements TestResult {

    private final Project project;
    private final ResourceProvider provider;
    private boolean isFaulty;
    private final long executed;
    private final long failed;

    /// Initializes a new default test result.
    ///
    /// @param project the project
    /// @param provider the provider of the test result, used to check 
    /// for equality
    /// @param name the name
    /// @param executed the executed
    /// @param failed the failed
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    protected DefaultTestResult(Project project, ResourceProvider provider,
            String name, long executed, long failed) {
        name(name);
        this.project = project;
        this.provider = provider;
        this.executed = executed;
        this.failed = failed;
    }

    /// Creates a new test result.
    ///
    /// @param <T> the resource type
    /// @param type the type
    /// @param project the project
    /// @param provider the provider
    /// @param name the name
    /// @param executed the number of tests executed
    /// @param failed the number of tests failed
    /// @return the object
    ///
    @SuppressWarnings({ "unchecked" })
    public static <T extends TestResult> T createTestResult(
            ResourceType<T> type, Project project, ResourceProvider provider,
            String name, long executed, long failed) {
        return (T) Proxy.newProxyInstance(type.rawType().getClassLoader(),
            new Class<?>[] { type.rawType(), Proxyable.class },
            new ForwardingHandler(new DefaultTestResult(project, provider,
                name, executed, failed)));
    }

    @Override
    public boolean isFaulty() {
        return isFaulty;
    }

    @Override
    public FaultAware setFaulty() {
        isFaulty = true;
        return null;
    }

    @Override
    public Project project() {
        return project;
    }

    @Override
    public long executed() {
        return executed;
    }

    @Override
    public long failed() {
        return failed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(provider);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof DefaultTestResult)) {
            return false;
        }
        DefaultTestResult other = (DefaultTestResult) obj;
        return Objects.equals(provider, other.provider);
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        String details;
        if (failed() > 0) {
            details = failed() + "/" + executed() + " failed";
        } else {
            details = executed() + " passed";
        }
        return TestResult.class.getSimpleName() + " from " + project().name()
            + " (" + name().get() + "): " + details;
    }
}
