/*
 * JDrupes Builder
 * Copyright (C) 2025, 2026 Michael N. Lipp
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

import java.nio.file.Path;

/// General project properties.
///
@SuppressWarnings("PMD.FieldNamingConventions")
public final class CoreProperties {

    /// The Build directory. Created artifacts should be put there.
    /// Defaults to [Path] "build".
    public static final PropertyKey<Path> BuildDirectory
        = new PropertyKey<>(Path.of("build"));

    /// The Encoding of files in the project.
    public static final PropertyKey<String> Encoding
        = new PropertyKey<>("UTF-8");

    /// The version of the project. Surprisingly, there is no
    /// agreed upon version type for Java (see e.g. 
    /// ["Version Comparison in Java"](https://www.baeldung.com/java-comparing-versions)).
    /// Therefore the version is represented as a string with "0.0.0"
    /// as default.
    public static final PropertyKey<String> Version
        = new PropertyKey<>("0.0.0");

    private CoreProperties() {
        // Make javadoc happy.
    }
}
