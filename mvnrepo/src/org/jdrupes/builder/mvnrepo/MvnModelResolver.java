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

import java.io.File;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/// A maven model resolver using aether.
///
public class MvnModelResolver implements ModelResolver {

    private final RepositorySystem repoSystem;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    /// Initializes a new maven model resolver.
    ///
    /// @param repoSystem the repo system
    /// @param session the session
    /// @param repositories the repositories
    ///
    public MvnModelResolver(RepositorySystem repoSystem,
            RepositorySystemSession session,
            List<RemoteRepository> repositories) {
        this.repoSystem = repoSystem;
        this.session = session;
        this.repositories = repositories;
    }

    @SuppressWarnings({ "deprecation", "PMD.AvoidDuplicateLiterals" })
    @Override
    public ModelSource resolveModel(String groupId, String artifactId,
            String version) throws UnresolvableModelException {
        return resolve(groupId, artifactId, version);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ModelSource resolveModel(Parent parent)
            throws UnresolvableModelException {
        // parent has groupId, artifactId, version
        return resolve(parent.getGroupId(), parent.getArtifactId(),
            parent.getVersion());
    }

    @SuppressWarnings("deprecation")
    @Override
    public ModelSource resolveModel(Dependency dependency)
            throws UnresolvableModelException {
        return resolve(dependency.getGroupId(), dependency.getArtifactId(),
            dependency.getVersion());
    }

    @SuppressWarnings("deprecation")
    private ModelSource resolve(String groupId, String artifactId,
            String version) throws UnresolvableModelException {
        try {
            ArtifactRequest request = new ArtifactRequest();
            request.setArtifact(
                new DefaultArtifact(groupId, artifactId, "pom", version));
            request.setRepositories(repositories);

            ArtifactResult result
                = repoSystem.resolveArtifact(session, request);
            File pomFile = result.getArtifact().getFile();
            return new FileModelSource(pomFile);
        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException(
                "Could not resolve POM for " + groupId + ":" + artifactId + ":"
                    + version,
                groupId, artifactId, version, e);
        }
    }

    @Override
    public void addRepository(Repository repository) {
        // Do nothing because we configure the repositories differently
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
        // Do nothing because we configure the repositories differently
    }

    @Override
    public ModelResolver newCopy() {
        return new MvnModelResolver(repoSystem, session, repositories);
    }
}
