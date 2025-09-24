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

import java.util.Objects;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.ResourceObject;

/// Represents an artifact in a maven repository.
///
public class DefaultMvnRepoResource extends ResourceObject
        implements MvnRepoResource {

    private final String groupId;
    private final String artifactId;
    private final String version;

    /// Instantiates a new default mvn repo dependency.
    ///
    /// @param type the type
    /// @param coordinate the coordinate
    ///
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public DefaultMvnRepoResource(ResourceType<? extends MvnRepoResource> type,
            String coordinate) {
        super(type);
        var parts = coordinate.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                "Invalid maven coordinate: " + coordinate);
        }
        groupId = parts[0];
        artifactId = parts[1];
        version = parts[2];
    }

    /// Group id.
    ///
    /// @return the string
    ///
    @Override
    public String groupId() {
        return groupId;
    }

    /// Artifact id.
    ///
    /// @return the string
    ///
    @Override
    public String artifactId() {
        return artifactId;
    }

    /// Version.
    ///
    /// @return the string
    ///
    @Override
    public String version() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
            + Objects.hash(artifactId, groupId, version, type());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DefaultMvnRepoResource other = (DefaultMvnRepoResource) obj;
        return Objects.equals(artifactId, other.artifactId)
            && Objects.equals(groupId, other.groupId)
            && Objects.equals(version, other.version)
            && Objects.equals(type(), other.type());
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
