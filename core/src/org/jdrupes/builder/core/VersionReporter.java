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

package org.jdrupes.builder.core;

import java.util.Collection;
import java.util.List;
import org.jdrupes.builder.api.CoreProperties;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ProjectVersion;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceType.ProjectVersionType;

/// A provider that reports the version of a project as a
/// [ProjectVersion] resource. The version is obtained from
/// the project's [Version property][CoreProperties#Version].
/// 
/// Add the generator to a project like this:
/// ```java
/// generator(VersionReporter::new);
///  ```
/// 
/// Also add a command alias that requests the [ProjectVersion] resource:
/// ```java
/// commandAlias("version")
///     .resources(of(ProjectVersionType).using(Supply));
/// ```
/// 
/// This makes it possible to query the project version from the command line
/// with:
/// ```bash
/// ./jdbld version
/// ```
///
public class VersionReporter extends AbstractGenerator {

    /// Instantiates a new version reporter.
    ///
    /// @param project the project
    ///
    public VersionReporter(Project project) {
        super(project);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <R extends Resource> Collection<R>
            doProvide(ResourceRequest<R> requested) {
        if (!requested.accepts(ProjectVersionType)) {
            return List.of();
        }
        var version = ProjectVersion.of(project(),
            project().get(CoreProperties.Version));
        return List.of((R) version);
    }
}
