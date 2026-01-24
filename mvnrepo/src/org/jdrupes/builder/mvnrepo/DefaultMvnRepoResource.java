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
    private String classifier = "";
    private String mvnType = "";
    private String version;

    /// Instantiates a new default mvn repo dependency. The coordinate is
    /// parsed into its component parts following the schema
    /// `groupId:artifactId[[:classifier]:type]:version`.
    ///
    /// @param type the type
    /// @param coordinate the coordinate
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public DefaultMvnRepoResource(ResourceType<? extends MvnRepoResource> type,
            String coordinate) {
        super(type);
        var parts = Objects.requireNonNull(coordinate).split(":");
        switch (parts.length) {
        case 5:
            classifier = parts[2];
            // fallthrough
        case 4:
            mvnType = parts[parts.length - 2];
            // fallthrough
        case 3:
            version = parts[parts.length - 1];
            // fallthrough
        case 2:
            artifactId = parts[1];
            groupId = parts[0];
            break;
        default:
            throw new IllegalArgumentException(
                "Invalid maven coordinate: " + coordinate);
        }
        name(coordinates());
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

    @Override
    public String classifier() {
        return classifier;
    }

    @Override
    public String mvnType() {
        return mvnType;
    }

    /// Version.
    ///
    /// @return the string (defaults to "")
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
            + Objects.hash(groupId, artifactId, classifier, mvnType, version);
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
        return (obj instanceof DefaultMvnRepoResource other)
            && Objects.equals(artifactId, other.artifactId)
            && Objects.equals(groupId, other.groupId)
            && Objects.equals(classifier, other.classifier)
            && Objects.equals(mvnType, other.mvnType)
            && Objects.equals(version, other.version);
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        return type().toString() + " " + coordinates();
    }
}
