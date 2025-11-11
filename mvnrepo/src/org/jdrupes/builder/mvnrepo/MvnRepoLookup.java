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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractProvider;
import org.jdrupes.builder.java.CompilationResources;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.mvnrepo.MvnRepoDependency.Scope;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

/// Depending on the request, this provider provides two types of resources.
/// 
///  1. The artifacts to be resolved as
///     `CompilationResources<MavenRepoDependencies>`. The artifacts
///     to be resolved are those added with [resolve].
///
///  2. The `CompilationResources<ClasspathElement>` 
///     or `RuntimeResources<ClasspathElement>` (depending on the
///     request) that result from resolving the artifacts to be resolved.
///
public class MvnRepoLookup extends AbstractProvider
        implements ResourceProvider {

    private final Map<ResourceType<? extends Resources<?>>,
            List<String>> coordinates = new ConcurrentHashMap<>();
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
    /// (`groupId:artifactId:version`) with the given scope.
    ///
    /// @param scope the scope
    /// @param coordinates the coordinates
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup resolve(Scope scope, String... coordinates) {
        this.coordinates
            .computeIfAbsent(scope == Scope.Compile ? MvnRepoCompilationDepsType
                : MvnRepoRuntimeDepsType, _ -> new ArrayList<>())
            .addAll(Arrays.asList(coordinates));
        return this;
    }

    /// Add artifacts, specified by their coordinates
    /// (`groupId:artifactId:version`) as compilation resources.
    ///
    /// @param coordinates the coordinates
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup resolve(String... coordinates) {
        return resolve(Scope.Compile, coordinates);
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
        if (requested.accepts(MvnRepoCompilationDepsType)) {
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) coordinates.entrySet().stream()
                .filter(e -> requested.accepts(e.getKey()))
                .map(e -> e.getValue().stream().map(c -> ResourceFactory
                    .create(MvnRepoDependencyType, null, c,
                        e.getKey().equals(MvnRepoCompilationDepsType)
                            ? Scope.Compile
                            : Scope.Runtime)))
                .flatMap(d -> d);
            return result;
        }
        if (requested
            .accepts(new ResourceType<CompilationResources<JarFile>>() {})) {
            return provideJars(requested);
        }
        return Stream.empty();
    }

    private <T extends Resource> Stream<T>
            provideJars(ResourceRequest<T> requested) {
        // Base collect request, optionally with snapshots
        CollectRequest collectRequest = new CollectRequest().setRepositories(
            new ArrayList<>(rootContext().remoteRepositories()));
        if (snapshotUri != null) {
            addSnapshotRepository(collectRequest);
        }

        // Retrieve coordinates and add to collect request
        var asDepsType = resourceType(
            requested.type().rawType(), MvnRepoDependency.class);
        coordinates.entrySet().stream()
            .filter(e -> asDepsType.isAssignableFrom(e.getKey()))
            .forEach(e -> e.getValue().stream().forEach(c -> collectRequest
                .addDependency(new Dependency(new DefaultArtifact(c),
                    e.getKey().equals(MvnRepoCompilationDepsType) ? "compile"
                        : "runtime"))));

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
                .map(p -> ResourceFactory.create(MvnRepoLibraryJarFileType, p));
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
