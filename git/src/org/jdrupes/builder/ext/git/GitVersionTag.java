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

import java.time.Instant;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import static org.jdrupes.builder.ext.git.GitTypes.*;

/// A resource that represents a Git version tag. Tags in Git are
/// global, i.e. they apply to all projects. Nevertheless, when using
/// independent versions for different projects, each project has its
/// own tag.
///
public interface GitVersionTag extends Resource {

    /// Creates a new Git version tag.
    ///
    /// @param project the project
    /// @param tag the tag
    /// @param asOf the timestamp of the tag
    /// @return the project version
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    static GitVersionTag of(Project project, String tag, Instant asOf) {
        return ResourceFactory.create(GitVersionTagType, project, tag, asOf);
    }

    /// Returns the project the tag is associated with.
    ///
    /// @return the project
    ///
    Project project();

    /// Returns the tag.
    ///
    /// @return the tag
    ///
    String tag();
}
