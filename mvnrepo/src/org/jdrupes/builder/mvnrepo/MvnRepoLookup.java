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
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
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
import org.jdrupes.builder.java.LibraryJarFile;
import org.jdrupes.builder.mvnrepo.MvnRepoDependency.Scope;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

/// Depending on the request, this provider provides two types of resources.
/// 
///  1. The artifacts to be resolved as
///     `Resources<MvnRepoDependency>` (or more specifically
///     `CompilationResources<MvnRepoDependency>` or
///     `RuntimeResources<MvnRepoDependency>`). The artifacts
///     to be resolved are those added with [resolve].
///
///  2. The BOMs added with [bom] as `Resources<MvnRepoBom>`.
///
///  3. The `CompilationResources<LibraryJarFile>` 
///     or `RuntimeResources<LibraryJarFile>` (depending on the
///     request) that result from resolving the artifacts to be resolved.
///     The resources returned implement the additional marker interface
///     `MvnRepoJarFile`.
///
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class MvnRepoLookup extends AbstractProvider
        implements ResourceProvider {

    private final Map<ResourceType<? extends Resources<?>>,
            List<String>> coordinates = new ConcurrentHashMap<>();
    private final List<String> boms = new ArrayList<>();
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

    /// Add a bill of materials. The coordinates are resolved as 
    /// a dependency with scope `import` which is added to the
    /// `dependencyManagement` section.
    ///
    /// @param coordinates the coordinates
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup bom(String... coordinates) {
        boms.addAll(Arrays.asList(coordinates));
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
        if (new ResourceType<Resources<MvnRepoResource>>() {}
            .isAssignableFrom(requested.type())) {
            return provideMvnDeps(requested);
        }
        if (requested.accepts(
            new ResourceType<CompilationResources<LibraryJarFile>>() {})) {
            try {
                return provideJars(requested);
            } catch (DependencyResolutionException | ModelBuildingException e) {
                throw new BuildException(
                    "Cannot resolve: " + e.getMessage(), e);
            }
        }
        return Stream.empty();
    }

    private <T extends Resource> Stream<T>
            provideMvnDeps(ResourceRequest<T> requested) {
        Stream<T> boms = Stream.empty();
        if (requested.accepts(new ResourceType<Resources<MvnRepoBom>>() {})) {
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) this.boms.stream().map(c -> ResourceFactory
                .create(MvnRepoBomType, null, c));
            boms = result;
        }
        Stream<T> deps = Stream.empty();
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
            deps = result;
        }
        return Stream.concat(boms, deps);
    }

    private <T extends Resource> Stream<T>
            provideJars(ResourceRequest<T> requested)
                    throws DependencyResolutionException,
                    ModelBuildingException {
        var repoSystem = rootContext().repositorySystem();
        var repoSession = rootContext().repositorySystemSession();
        var repos = new ArrayList<>(rootContext().remoteRepositories());
        if (snapshotUri != null) {
            repos.add(createSnapshotRepository());
        }

        // Create an effective model. This make sure that BOMs are
        // handled correctly.
        Model model = getEffectiveModel(repoSystem, repoSession,
            repos, requested);

        // Create collect request using data from the (effective) model
        CollectRequest collectRequest
            = new CollectRequest().setRepositories(repos);
        collectRequest.setManagedDependencies(
            model.getDependencyManagement().getDependencies().stream()
                .map(DependencyConverter::convert).toList());
        model.getDependencies().stream().map(DependencyConverter::convert)
            .forEach(collectRequest::addDependency);

        // Resolve dependencies
        DependencyRequest dependencyRequest
            = new DependencyRequest(collectRequest, null);
        DependencyNode rootNode;
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
    }

    private Model getEffectiveModel(RepositorySystem repoSystem,
            RepositorySystemSession repoSession, List<RemoteRepository> repos,
            ResourceRequest<?> requested)
            throws ModelBuildingException {
        // First build raw model
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId("model.group");
        model.setArtifactId("model.artifact");
        model.setVersion("0.0.0");
        var depMgmt = new DependencyManagement();
        model.setDependencyManagement(depMgmt);
        boms.stream().forEach(c -> {
            var mvnResource = new DefaultMvnRepoDependency(
                MvnRepoDependencyType, c, Scope.Runtime);
            var dep = DependencyConverter.convert(mvnResource);
            dep.setScope("import");
            dep.setType("pom");
            depMgmt.addDependency(dep);
        });
        var asDepsType
            = resourceType(requested.type().rawType(), MvnRepoDependency.class);
        coordinates.entrySet().stream()
            .filter(e -> asDepsType.isAssignableFrom(e.getKey()))
            .forEach(e -> e.getValue().stream().forEach(c -> {
                var mvnResource = new DefaultMvnRepoDependency(
                    MvnRepoDependencyType, c,
                    e.getKey().equals(MvnRepoCompilationDepsType)
                        ? Scope.Compile
                        : Scope.Runtime);
                model.addDependency(DependencyConverter.convert(mvnResource));
            }));

        // Now build (derive) effective model
        var buildingRequest = new DefaultModelBuildingRequest();
        buildingRequest.setRawModel(model);
        buildingRequest.setProcessPlugins(false);
        buildingRequest
            .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        var resolver = new MvnModelResolver(repoSystem, repoSession, repos);
        buildingRequest.setModelResolver(resolver);
        return new DefaultModelBuilderFactory()
            .newInstance().build(buildingRequest).getEffectiveModel();
    }

    private RemoteRepository createSnapshotRepository() {
        return new RemoteRepository.Builder(
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
