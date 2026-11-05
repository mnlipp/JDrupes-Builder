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

package org.jdrupes.builder.ext.checkstyle;

import org.jdrupes.builder.api.FaultAware;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceProvider;

/// Provides the results from running checkstyle.
///
public interface CheckstyleResult extends Resource, FaultAware {

    /// The project that was checked.
    ///
    /// @return the project
    ///
    Project project();

    /// Returns the number of checked files.
    ///
    /// @return the number
    ///
    long checked();

    /// Returns the number of style violations with error severity.
    ///
    /// @return the number
    ///
    long errors();

    /// Returns the number of style violations with warning severity.
    ///
    /// @return the number
    ///
    long warnings();

    /// Creates a new checkstyle result from the given values.
    ///
    /// @param project the project
    /// @param provider the provider
    /// @param checked the number of checked files
    /// @param errors the number of style violations with error severity
    /// @param warnings the number of style violations with warning severity
    /// @return the checkstyle result
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    static CheckstyleResult of(Project project, ResourceProvider provider,
            long checked, long errors, long warnings) {
        return ResourceFactory.create(CheckstyleTypes.CheckstyleResultType,
            project, provider, checked, errors, warnings);
    }
}
