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

package org.jdrupes.builder.ext.git;

import java.util.Objects;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.ResourceObject;

/// Represents a Git version tag.
///
public class DefaultGitVersionTag extends ResourceObject
        implements GitVersionTag {

    private final Project project;
    private final String versionTag;

    /// Initializes a new Git version tag.
    ///
    /// @param project the project
    /// @param versionTag the version tag
    ///
    public DefaultGitVersionTag(Project project, String versionTag) {
        this.project = project;
        this.versionTag = versionTag;
    }

    @Override
    public Project project() {
        return project;
    }

    @Override
    public String tag() {
        return versionTag;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(project, versionTag);
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
        if (!(obj instanceof DefaultGitVersionTag)) {
            return false;
        }
        DefaultGitVersionTag other = (DefaultGitVersionTag) obj;
        return Objects.equals(project, other.project)
            && Objects.equals(versionTag, other.versionTag);
    }

    @Override
    public String toString() {
        return String.format("%s from %s: %s",
            GitVersionTag.class.getSimpleName(), project.name(), versionTag);
    }

}
