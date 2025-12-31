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
import org.jdrupes.builder.api.Proxyable;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.TestResult;

/// Default implementation of a test result.
///
public class DefaultTestResult extends ResourceObject implements TestResult {

    private final String name;
    private final int executed;
    private final int failed;

    /// Initializes a new default test result.
    ///
    /// @param name the name
    /// @param executed the executed
    /// @param failed the failed
    ///
    protected DefaultTestResult(String name, int executed, int failed) {
        super();
        this.name = name;
        this.executed = executed;
        this.failed = failed;
    }

    /// Creates a new test result.
    ///
    /// @param <T> the resource type
    /// @param type the type
    /// @param name the name
    /// @param executed the number of tests executed
    /// @param failed the number of tests failed
    /// @return the object
    ///
    @SuppressWarnings({ "unchecked" })
    public static <T extends TestResult> T createTestResult(
            ResourceType<T> type, String name, int executed, int failed) {
        return (T) Proxy.newProxyInstance(type.rawType().getClassLoader(),
            new Class<?>[] { type.rawType(), Proxyable.class },
            new ForwardingHandler(
                new DefaultTestResult(name, executed, failed)));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int executed() {
        return executed;
    }

    @Override
    public int failed() {
        return failed;
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        return type().toString() + " " + name() + ": failed " + failed() + "/"
            + executed();
    }
}
