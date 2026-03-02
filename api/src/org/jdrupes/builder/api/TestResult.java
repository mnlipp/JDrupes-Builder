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

package org.jdrupes.builder.api;

import java.util.Optional;
import static org.jdrupes.builder.api.ResourceType.TestResultType;

/// Provides the results from running tests.
///
public interface TestResult extends Resource, FaultAware {

    /// The test project.
    ///
    /// @return the project
    ///
    Project project();

    /// The name of the test or test suite.
    ///
    /// @return the name
    ///
    @Override
    Optional<String> name();

    /// Returns the number of executed tests.
    ///
    /// @return the number
    ///
    long executed();

    /// Returns the number of test failures.
    ///
    /// @return the number
    ///
    long failed();

    /// Creates a new test result from the given values.
    ///
    /// @param project the project
    /// @param provider the provider
    /// @param name the name
    /// @param executed the executed
    /// @param failed the failed
    /// @return the test result
    ///
    static TestResult from(Project project, ResourceProvider provider,
            String name, long executed, long failed) {
        return ResourceFactory.create(
            TestResultType, project, provider, name, executed, failed);
    }
}
