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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.core.AbstractProvider;
import org.jdrupes.builder.java.CompilationResources;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.RuntimeResources;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

/// Depending on the request, this provider provides two types of resources.
/// 
///  1. The artifacts to be resolved as [MvnRepoDependencies]. The artifacts
///     to be resolved are those added with [resolve].
///
///  2. The [CompilationResources] or [RuntimeResources] (depending on the
///     request) that result from resolving the artifacts to be resolved.
///
public class MvnRepoLookup extends AbstractProvider
        implements ResourceProvider {

    private final List<String> coordinates = new ArrayList<>();
    private boolean downloadSources = true;
    private boolean downloadJavadoc = true;
    private URI snapshotUri;
    private static Context rootContextInstance;

    /// Instantiates a new mvn repo lookup.
    ///
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public MvnRepoLookup() {
        // Make javadoc happy.
    }

    /// Lazily creates the root context.
    /// @return the context
    ///
    /* default */ static Context rootContext() {
        if (rootContextInstance != null) {
            return rootContextInstance;
        }
        ContextOverrides overrides = ContextOverrides.create()
            .withUserSettings(true).build();
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        rootContextInstance = runtime.create(overrides);
        return rootContextInstance;
    }

    /// Sets the Maven snapshot repository URI.
    ///
    /// @param uri the snapshot repository URI
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup snapshotRepository(URI uri) {
        this.snapshotUri = uri;
        return this;
    }

    /// Returns the snapshot repository. Defaults to
    /// `https://central.sonatype.com/repository/maven-snapshots/`.
    ///
    /// @return the uri
    ///
    public URI snapshotRepository() {
        return snapshotUri;
    }

    /// Add artifacts, specified by their coordinates
    /// (`groupId:artifactId:version`).
    ///
    /// @param coordinates the coordinates
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup resolve(String... coordinates) {
        this.coordinates.addAll(Arrays.asList(coordinates));
        return this;
    }

    /// Whether to also download the sources. Defaults to `true`.
    ///
    /// @param enable the enable
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup downloadSources(boolean enable) {
        this.downloadSources = enable;
        return this;
    }

    /// Whether to also download the javadoc. Defaults to `true`.
    ///
    /// @param enable the enable
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup downloadJavadoc(boolean enable) {
        this.downloadJavadoc = enable;
        return this;
    }

    /// Provide.
    ///
    /// @param <T> the generic type
    /// @param requested the requested resources
    /// @return the stream
    ///
    @Override
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (requested.wants(MvnRepoDependenciesType)) {
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) coordinates.stream()
                .map(c -> ResourceFactory.create(MvnRepoDependencyType, null,
                    c));
            return result;
        }
        if (requested.wants(CompilationResourcesType)
            && requested.includes(JarFileType)) {
            return provideJars(requested);
        }
        return Stream.empty();
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private <T extends Resource> Stream<T>
            provideJars(ResourceRequest<T> requested) {
        CollectRequest collectRequest = new CollectRequest().setRepositories(
            new ArrayList<>(rootContext().remoteRepositories()));
        if (snapshotUri != null) {
            addSnapshotRepository(collectRequest);
        }
        for (var coord : coordinates) {
            collectRequest.addDependency(
                new Dependency(new DefaultArtifact(coord),
                    requested.wants(CompilationResourcesType) ? "compile"
                        : "runtime"));
        }

        DependencyRequest dependencyRequest
            = new DependencyRequest(collectRequest, null);
        DependencyNode rootNode;
        try {
            var repoSystem = rootContext().repositorySystem();
            var repoSession = rootContext().repositorySystemSession();
            rootNode = repoSystem.resolveDependencies(repoSession,
                dependencyRequest).getRoot();
// For maven 2.x libraries:
//                List<DependencyNode> dependencyNodes = new ArrayList<>();
//                rootNode.accept(new PreorderDependencyNodeConsumerVisitor(
//                    dependencyNodes::add));
            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            rootNode.accept(nlg);
            List<DependencyNode> dependencyNodes = nlg.getNodes();
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) dependencyNodes.stream()
                .filter(d -> d.getArtifact() != null)
                .map(DependencyNode::getArtifact)
                .map(a -> {
                    if (downloadSources) {
                        downloadSourceJar(repoSystem, repoSession, a);
                    }
                    if (downloadJavadoc) {
                        downloadJavadocJar(repoSystem, repoSession, a);
                    }
                    return a;
                }).map(a -> a.getFile().toPath())
                .map(p -> ResourceFactory.create(MvnRepoJarFileType, p));
            return result;
        } catch (DependencyResolutionException e) {
            throw new BuildException(
                "Cannot resolve: " + e.getMessage(), e);
        }
    }

    private void addSnapshotRepository(CollectRequest collectRequest) {
        RemoteRepository snapshotsRepo = new RemoteRepository.Builder(
            "snapshots", "default", snapshotUri.toString())
                .setSnapshotPolicy(new RepositoryPolicy(
                    true,  // enable snapshots
                    RepositoryPolicy.UPDATE_POLICY_ALWAYS,
                    RepositoryPolicy.CHECKSUM_POLICY_WARN))
                .setReleasePolicy(new RepositoryPolicy(
                    false,
                    RepositoryPolicy.UPDATE_POLICY_NEVER,
                    RepositoryPolicy.CHECKSUM_POLICY_IGNORE))
                .build();
        collectRequest.addRepository(snapshotsRepo);
    }

    private void downloadSourceJar(RepositorySystem repoSystem,
            RepositorySystemSession repoSession, Artifact jarArtifact) {
        Artifact sourcesArtifact
            = new SubArtifact(jarArtifact, "sources", "jar");
        ArtifactRequest sourcesRequest = new ArtifactRequest();
        sourcesRequest.setArtifact(sourcesArtifact);
        sourcesRequest.setRepositories(rootContext().remoteRepositories());
        try {
            repoSystem.resolveArtifact(repoSession, sourcesRequest);
        } catch (ArtifactResolutionException e) { // NOPMD
            // Ignore, sources are optional
        }
    }

    private void downloadJavadocJar(RepositorySystem repoSystem,
            RepositorySystemSession repoSession, Artifact jarArtifact) {
        Artifact javadocArtifact
            = new SubArtifact(jarArtifact, "javadoc", "jar");
        ArtifactRequest sourcesRequest = new ArtifactRequest();
        sourcesRequest.setArtifact(javadocArtifact);
        sourcesRequest.setRepositories(rootContext().remoteRepositories());
        try {
            repoSystem.resolveArtifact(repoSession, sourcesRequest);
        } catch (ArtifactResolutionException e) { // NOPMD
            // Ignore, javadoc is optional
        }
    }
}
