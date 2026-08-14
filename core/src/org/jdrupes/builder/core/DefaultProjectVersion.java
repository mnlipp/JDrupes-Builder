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

package org.jdrupes.builder.core;

import java.util.Objects;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ProjectVersion;

/// Default implementation of [ProjectVersion].
///
public class DefaultProjectVersion extends ResourceObject
        implements ProjectVersion {

    private final Project project;
    private final String version;

    /// Initializes a new default project version.
    ///
    /// @param project the project
    /// @param version the version
    ///
    protected DefaultProjectVersion(Project project, String version) {
        this.project = Objects.requireNonNull(project);
        this.version = Objects.requireNonNull(version);
    }

    @Override
    public Project project() {
        return project;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(project, version);
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
        if (!(obj instanceof DefaultProjectVersion)) {
            return false;
        }
        DefaultProjectVersion other = (DefaultProjectVersion) obj;
        return Objects.equals(project, other.project)
            && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        var dir = project().rootProject().relativize(project().directory())
            .toString();
        return project().name() + ": " + version() + " ("
            + (dir.isBlank() ? "." : dir) + ")";
    }
}
