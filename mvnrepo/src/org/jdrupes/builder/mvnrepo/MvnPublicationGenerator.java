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

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.stream.Stream;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jdrupes.builder.api.BuildException;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.java.LibraryJarFile;

import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

public class MvnPublicationGenerator extends AbstractGenerator {

    private URI repositoryUri;
    private URI snapshotUri;
    private String repoUser;
    private String repoPass;

    public MvnPublicationGenerator(Project project) {
        super(project);
        repositoryUri = URI.create(MvnRepoLookup.rootContext()
            .remoteRepositories().get(0).getUrl());
    }

    public MvnPublicationGenerator repository(URI uri) {
        this.repositoryUri = uri;
        return this;
    }

    public MvnPublicationGenerator snapshotRepository(URI uri) {
        this.snapshotUri = uri;
        return this;
    }

    public MvnPublicationGenerator credentials(String user, String pass) {
        this.repoUser = user;
        this.repoPass = pass;
        return this;
    }

    @Override
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.includes(MvnPublicationType)) {
            return Stream.empty();
        }
        var pomResource = project().supplied(new ResourceRequest<PomFile>(
            new ResourceType<>() {})).findFirst();
        if (pomResource.isEmpty()) {
            log.warning("No POM file resource available.");
            return Stream.empty();
        }
        var jarResource = project().getFrom(project()
            .providers(Expose, Forward),
            new ResourceRequest<LibraryJarFile>(
                new ResourceType<>() {}))
            .findFirst();
        if (jarResource.isEmpty()) {
            log.warning("No jar file resource available.");
            return Stream.empty();
        }

        // Get the coordinates from the model
        try {
            var pomFile = pomResource.get().path().toFile();
            var req = new DefaultModelBuildingRequest().setPomFile(pomFile);
            var model = new DefaultModelBuilderFactory().newInstance()
                .build(req).getEffectiveModel();
            var pomArtifact = new DefaultArtifact(model.getGroupId(),
                model.getArtifactId(), "pom", model.getVersion())
                    .setFile(pomFile);
            var isSnapshot = pomArtifact.isSnapshot();
            var repo = new RemoteRepository.Builder(
                "mine", "default",
                (isSnapshot ? snapshotUri : repositoryUri)
                    .toString())
                        .setAuthentication(new AuthenticationBuilder()
                            .addUsername(repoUser).addPassword(repoPass)
                            .build())
                        .build();
            var deployReq = new DeployRequest().setRepository(repo)
                .addArtifact(pomArtifact).addArtifact(new DefaultArtifact(
                    model.getGroupId(), model.getArtifactId(), "jar",
                    model.getVersion())
                        .setFile(jarResource.get().path().toFile()));

            @SuppressWarnings("PMD.CloseResource")
            var context = MvnRepoLookup.rootContext();
            var result = context.repositorySystem().deploy(
                context.repositorySystemSession(), deployReq);
            return (Stream<T>) Stream
                .of(project().newResource(MvnPublicationType,
                    model.getGroupId() + ":" + model.getArtifactId() + ":"
                        + model.getVersion()));
        } catch (ModelBuildingException | DeploymentException e) {
            throw new BuildException(e);
        }

    }

}
