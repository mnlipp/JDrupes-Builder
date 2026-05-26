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

package org.jdrupes.builder.java.internal;

import java.util.ArrayList;
import java.util.List;

/// Represents the configuration data for an application.
///
@SuppressWarnings("PMD.DataClass")
public class ApplicationConfigurationData {

    private String executableName;
    private String mainClassName;
    private final List<String> defaultJvmArgs = new ArrayList<>();

    /// Initializes a new distribution configuration data.
    ///
    public ApplicationConfigurationData() {
        // Make javadoc happy.
    }

    /// Returns the name of the generated executable.
    ///
    /// @return the string
    ///
    public String executableName() {
        return executableName;
    }

    /// Sets the name of the generated executable.
    ///
    /// @param name the name
    ///
    public void executableName(String name) {
        executableName = name;
    }

    /// Returns the main class name.
    ///
    /// @return the string
    ///
    public String mainClassName() {
        return mainClassName;
    }

    /// Sets the main class name.
    ///
    /// @param name the name
    ///
    public void mainClassName(String name) {
        mainClassName = name;
    }

    /// Returns a mutable list of JVM arguments.
    ///
    /// @return the list
    ///
    public List<String> applicationJvmOpts() {
        return defaultJvmArgs;
    }
}
