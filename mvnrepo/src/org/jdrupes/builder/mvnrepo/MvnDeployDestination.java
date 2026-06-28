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

import com.google.common.flogger.FluentLogger;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jdrupes.builder.api.BuildContext;
import org.jdrupes.builder.api.BuildException;

/// A Maven publishing destination that uploads artifacts using the traditional
/// Maven deployment approach.
///
/// This implementation of a [MvnPublishingDestination] uploads artifacts
/// and their associated metadata (like `maven-metadata.xml`) individually
/// to the specified repository URI. It is typically used for publishing
/// snapshots or deploying to internal Maven repositories.
///
public class MvnDeployDestination extends MvnPublishingDestination {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private String id;
    private URI repositoryUri;

    /// Initializes a new Maven deploy destination.
    ///
    /// @param publicationTypes the supported publication types
    ///
    public MvnDeployDestination(PublicationType... publicationTypes) {
        super(publicationTypes);
        if (publicationTypes.length == 1
            && publicationTypes[0] == PublicationType.SNAPSHOT) {
            repositoryUri = URI.create(
                "https://central.sonatype.com/repository/maven-snapshots/");
        }
    }

    /// Sets the Maven repository URI.
    ///
    /// @param uri the snapshot repository URI
    /// @return the Maven publisher
    ///
    public MvnDeployDestination repositoryUri(URI uri) {
        this.repositoryUri = uri;
        return this;
    }

    /// Returns the repository URI. Defaults to
    /// `https://central.sonatype.com/repository/maven-snapshots/` if
    /// this deploy destination has been created with publication type
    /// `SNAPSHOT`.
    ///
    /// @return the uri
    ///
    public URI repositoryUri() {
        return repositoryUri;
    }

    @Override
    /* default */void publish(BuildContext context, MvnPublisher publisher,
            Artifact mainArtifact, List<Artifact> toDeploy) {
        // Now deploy everything
        var mvnContext = MvnRepoLookup.mavenContext();
        var session = new DefaultRepositorySystemSession(
            mvnContext.repositorySession());
        session.setRepositoryListener(new UploadListener(
            context, mainArtifact.getGroupId() + ":"
                + mainArtifact.getArtifactId()
                + ":" + mainArtifact.getVersion(),
            toDeploy.size()));
        var user = repositoryUser(context);
        var password = repositoryPassword(context);
        var repo = new RemoteRepository.Builder("deploy", "default",
            repositoryUri.toString())
                .setAuthentication(new AuthenticationBuilder()
                    .addUsername(user).addPassword(password).build())
                .build();
        var deployReq = new DeployRequest().setRepository(repo);
        toDeploy.stream().forEach(deployReq::addArtifact);
        try {
            mvnContext.repositorySystem().deploy(session, deployReq);
        } catch (DeploymentException e) {
            throw new BuildException().from(publisher).cause(e);
        }
    }

    @SuppressWarnings("PMD.CommentRequired")
    private final class UploadListener extends AbstractRepositoryListener {
        private final AtomicBoolean startMsgLogged = new AtomicBoolean(false);
        private final AtomicInteger deployedCount = new AtomicInteger(0);
        private final String artifact;
        private final BuildContext context;
        private final int artifacts;

        @SuppressWarnings("PMD.PublicMemberInNonPublicType")
        public UploadListener(BuildContext context, String artifact,
                int artifacts) {
            this.context = context;
            this.artifact = artifact;
            this.artifacts = artifacts;
        }

        @Override
        public void artifactDeploying(RepositoryEvent event) {
            if (!startMsgLogged.getAndSet(true)) {
                logger.atInfo().log("Start deploying artifacts...");
                context.statusLine().update(
                    "%s deploys to %s", this, repositoryUri);
            }
        }

        @Override
        public void artifactDeployed(RepositoryEvent event) {
            if (!"jar".equals(event.getArtifact().getExtension())) {
                return;
            }
            logger.atInfo().log("Deployed: %s", event.getArtifact());
            context.statusLine().update("%s deployed %d/%d", this,
                deployedCount.incrementAndGet(), artifacts);
        }

        @Override
        public void metadataDeployed(RepositoryEvent event) {
            logger.atInfo().log("Deployed: %s", event.getMetadata());
            context.statusLine().update("%s deployed %d/%d", this,
                deployedCount.incrementAndGet(), artifacts);
        }

        @Override
        public String toString() {
            return "Maven deployer for " + artifact;
        }

    }

    @Override
    public String toString() {
        return "Maven deploy destination " + (id != null ? (id + "::")
            : "") + repositoryUri;
    }
}
