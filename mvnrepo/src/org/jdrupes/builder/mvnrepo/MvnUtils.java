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

package org.jdrupes.builder.mvnrepo;

import java.util.List;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;

/// The Class MvnUtils.
///
public final class MvnUtils {

    private MvnUtils() {
    }

    /// Checks if the version is a range.
    ///
    /// @param version the version
    /// @return true, if is version range
    ///
    /* default */ static boolean isVersionRange(String version) {
        return version.startsWith("[") || version.startsWith("(");
    }

    /// Resolve version range.
    /// @param repositories the repositories
    /// @param groupId the group id
    /// @param artifactId the artifact id
    /// @param versionRange the version range
    ///
    /// @return the string
    ///
    /* default */ static String resolveVersionRange(
            List<RemoteRepository> repositories,
            String groupId, String artifactId, String versionRange) {
        var versionRangeRequest = new VersionRangeRequest();
        versionRangeRequest.setArtifact(new DefaultArtifact(groupId,
            artifactId, "", versionRange));
        versionRangeRequest.setRepositories(repositories);
        try {
            VersionRangeResult result = MavenContext.repositorySystem()
                .resolveVersionRange(MavenContext.repositorySession(),
                    versionRangeRequest);
            var resolved = result.getHighestVersion();
            if (resolved != null) {
                return resolved.toString();
            }
        } catch (VersionRangeResolutionException e) { // NOPMD
            // Fall through and let artifact resolution fail with a message
        }
        return versionRange;
    }

}
