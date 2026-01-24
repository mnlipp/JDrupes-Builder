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

/// Provides the results from running tests.
///
public interface TestResult extends Resource {

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
}
