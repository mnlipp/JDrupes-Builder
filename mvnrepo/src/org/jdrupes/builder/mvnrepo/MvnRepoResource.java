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

package org.jdrupes.builder.mvnrepo;

import org.jdrupes.builder.api.Resource;

/// Represents a dependency on a maven artifact obtainable from a
/// maven repository.
///
public interface MvnRepoResource extends Resource {

    /// Group id.
    ///
    /// @return the string
    ///
    String groupId();

    /// Artifact id.
    ///
    /// @return the string
    ///
    String artifactId();

    /// Classifier.
    ///
    /// @return the string (defaults to "")
    ///
    String classifier();

    /// Maven type.
    ///
    /// @return the string (defaults to "")
    ///
    String mvnType();

    /// Version.
    ///
    /// @return the string
    ///
    String version();

    /// Return the maven coordinates as "groudId:artifactId:version".
    ///
    /// @return the string
    ///
    default String coordinates() {
        return groupId() + ":" + artifactId()
            + (classifier().isBlank() ? "" : ":" + classifier())
            + (mvnType().isBlank() ? "" : ":" + mvnType())
            + ":" + version();

    }
}
