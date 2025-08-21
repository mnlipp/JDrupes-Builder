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

/// A marker interface to identify the root project.
///
public interface RootProject extends Project {

    /// May be overridden by the root project to apply common settings
    /// to projects of specific types or with specific properties.
    /// 
    /// This method must be invoked by any base class for project 
    /// configuration classes before it returns the control to the
    /// project configuration class' constructor. The method is never
    /// invoked by the user.
    ///
    /// @param project the project to prepare
    ///
    default void prepareProject(Project project) {
        // Default does nothing
    }

    /// Define an alias for requesting one more more specific resources.
    ///
    /// @param name the name
    /// @param requests the requests
    /// @return the root project
    ///
    default RootProject commandAlias(String name,
            ResourceRequest<?>... requests) {
        throw new UnsupportedOperationException();
    }
}
