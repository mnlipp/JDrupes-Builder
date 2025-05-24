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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/// Represents a resource.
///
public interface Resource {

    /// The kind unknown.
    String KIND_UNKNOWN = "unknown";

    /// The kind resource.
    String KIND_RESOURCE = "resource";

    /// The kind resource.
    String KIND_RESOURCES = "resource directory";

    /// The kind jvm class.
    String KIND_CLASS = "class";

    /// The kind class directory.
    String KIND_CLASSES = "class directory";

    /// The kind library jar.
    String KIND_LIB_JAR = "library jar";

    /// The kind application jar.
    String KIND_APP_JAR = "application jar";

    /// The instant at which this resource was created or last modified.
    ///
    /// @return the instant
    ///
    default Instant asOf() {
        return Instant.MIN;
    }

    /// Returns the kind of this resource.
    ///
    /// @return the kind
    ///
    default String kind() {
        return KIND_UNKNOWN;
    }

    /// Returns a localized string representation of the instant
    /// at which this resource was created or last modified.
    ///
    /// @return the string
    ///
    default String asOfLocalized() {
        return DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .format(asOf().atZone(ZoneId.systemDefault()));
    }
}
