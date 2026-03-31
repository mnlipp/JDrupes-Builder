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

import java.io.IOException;
import java.util.stream.Stream;

/// Additional methods for the root project. Root projects are always
/// created with an associated [BuildContext]. This context is closed
/// when the root project is closed.
///
public interface RootProject extends Project, AutoCloseable {

    /// May be overridden by the root project to apply common settings
    /// to projects of specific types or with specific properties.
    /// 
    /// This method must be invoked by any base class for project 
    /// configuration classes before it returns the control to the
    /// project configuration class' constructor. The method is never
    /// invoked by the user.
    /// 
    /// This method is provided for convenience. Alternatively, project
    /// configuration classes can invoke any user defined method at the
    /// beginning of their constructor. Using this method simply makes
    /// sure that such an invocation is never forgotten.
    ///
    /// @param project the project to prepare
    /// @throws Exception the exception
    ///
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    default void prepareProject(Project project) throws Exception {
        // Default does nothing
    }

    /// Return the projects matching the patterns. A pattern is a glob
    /// pattern applied to the project's directory. `""`matches the root
    /// project. `"*"` matches the root project and all immediate
    /// sub project. `"**"` matches all projects.
    ///
    /// @param patterns the patterns
    /// @return the stream
    ///
    default Stream<Project> projects(String... patterns) {
        return projects(patterns, new String[0]);
    }

    /// Return the projects matching the patterns but not matching any
    /// of the pattern in the `without` parameter. A pattern is a glob
    /// pattern applied to the project's directory. `""`matches the root
    /// project. `"*"` matches the root project and all immediate
    /// sub project. `"**"` matches all projects.
    ///
    /// @param patterns the patterns
    /// @param without patterns to exclude
    /// @return the stream
    ///
    @SuppressWarnings("PMD.UseVarargs")
    Stream<Project> projects(String[] patterns, String[] without);

    /// Define an alias for requesting one or more specific resources.
    ///
    /// @param name the name
    /// @return the root project
    ///
    default CommandBuilder commandAlias(String name) {
        throw new UnsupportedOperationException();
    }

    /// Close the project. The re-declaration of this method removes
    /// the [IOException], which is never thrown.
    ///
    @Override
    void close();

    /// A builder for command aliases.
    interface CommandBuilder {

        /// Apply the request(s) to the projects selected by the given
        /// patterns instead of the root project.
        ///
        /// @param patterns the patterns
        /// @return the command builder
        ///
        CommandBuilder projects(String... patterns);

        /// Do not apply the request(s) to the projects selected by the
        /// given patterns.
        ///
        /// @param patterns the patterns
        /// @return the command builder
        ///
        CommandBuilder without(String... patterns);

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
