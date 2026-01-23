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

import java.util.stream.Stream;

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
    /// @throws Exception the exception
    ///
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    default void prepareProject(Project project) throws Exception {
        // Default does nothing
    }

    /// Return the projects matching the pattern. The pattern is a glob
    /// pattern applied to the project's directory. `""`matches the root
    /// project. `"*"` matches the root project and all immediate
    /// sub project. `"**"` matches all projects.
    ///
    /// @param pattern the pattern
    /// @return the stream
    ///
    Stream<Project> projects(String pattern);

    /// Define an alias for requesting one or more specific resources.
    ///
    /// @param name the name
    /// @return the root project
    ///
    default CommandBuilder commandAlias(String name) {
        throw new UnsupportedOperationException();
    }

    /// A builder for command aliases.
    interface CommandBuilder {

        /// Apply the request(s) to the projects selected by the given
        /// pattern instead of the root project.
        ///
        /// @param pattern the pattern
        /// @return the command builder
        ///
        CommandBuilder projects(String pattern);

        /// Apply the request(s) to the root project or the selected
        /// projects. The results from the request are written to the
        /// standard output.
        /// 
        /// To simplify the command alias definition, this method
        /// replaces a request with no intents (i.e. with
        /// [ResourceRequest#uses()] being empty) with a request that
        /// uses all intents.
        ///
        /// @param requests the requests
        /// @return the root project
        ///
        RootProject resources(ResourceRequest<?>... requests);
    }
}
