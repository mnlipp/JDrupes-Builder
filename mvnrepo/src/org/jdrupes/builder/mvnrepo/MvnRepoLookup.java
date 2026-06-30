/*
 * JDrupes Builder
 * Copyright (C) 2025, 2026 Michael N. Lipp
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
import static com.google.common.flogger.LazyArgs.lazy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
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
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.graph.visitor.PreorderDependencyNodeConsumerVisitor;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.core.AbstractProvider;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

/// Depending on the request, this provider provides two types of resources.
/// 
///  1. The artifacts to be resolved as resources of type [MvnRepoDependency].
///     The artifacts to be resolved are those added with [resolve].
///     Note that the result also includes the [MvnRepoBom]s added with
///     [bom].
///
///  2. The resources of type [MvnRepoLibraryJarFile] that result from
///     resolving the artifacts to be resolved.
///
/// The repositories used are those added with [addRepositories]. If
/// no repositories are configured, the Maven Central repository
/// is added automatically.
/// 
/// Resolving is performed using Maven Resolver (formerly Eclipse Aether)
/// version 2.x. Dependencies are collected from the specified artifacts after
/// evaluating their effective Maven models, including any imported
/// BOMs. Version conflicts are resolved using a "highest wins"
/// strategy, i.e. the highest version of a dependency encountered in the
/// dependency graph is selected. Note that this differs from Maven's
/// default behavior which is "nearest wins".
/// 
/// Results of the dependency resolution are written to the log with
/// log level FINE.
/// 
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class MvnRepoLookup extends AbstractProvider {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final List<RemoteRepository> addedRepos = new ArrayList<>();
    private final List<String> coordinates = new ArrayList<>();
    private final List<String> boms = new ArrayList<>();
    private boolean downloadSources = true;
    private boolean downloadJavadoc = true;
    private boolean probeMode;

    /// Initializes a new Maven repository lookup.
    ///
    public MvnRepoLookup() {
        // Make javadoc happy.
    }

    /// Add repositories to be used for the lookup.
    ///
    /// @param repositories the repositories
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup addRepositories(RemoteRepository... repositories) {
        addedRepos.addAll(Arrays.asList(repositories));
        return this;

    }

    /// Add a repository that is to be used for the lookup.
    ///
    /// @param id the repository id
    /// @param uri the repository uri
    /// @param supported the supported version types
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup addRepository(
            String id, URI uri, MvnVersionType... supported) {
        var types = EnumSet.copyOf(Arrays.asList(supported));
        var builder = new RemoteRepository.Builder(
            id, "default", uri.toString())
                .setReleasePolicy(
                    MavenContext.createPolicy(MvnVersionType.RELEASE,
                        types.contains(MvnVersionType.RELEASE), null, null))
                .setSnapshotPolicy(
                    MavenContext.createPolicy(MvnVersionType.SNAPSHOT,
                        types.contains(MvnVersionType.SNAPSHOT), null, null));
        addedRepos.add(builder.build());
        return this;
    }

    /// Add a bill of materials. The coordinates are resolved as 
    /// a dependency with scope `import` which is added to the
    /// `dependencyManagement` section when evaluating the effective
    /// model.
    ///
    /// @param coordinates the coordinates in the form
    /// groupId:artifactId:version
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup bom(String... coordinates) {
        boms.addAll(Arrays.asList(coordinates));
        return this;
    }

    /// Add artifacts, specified by their coordinates
    /// (`groupId:artifactId:version`) as resources.
    ///
    /// @param coordinates the coordinates in the form
    /// groupId:artifactId:version
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup resolve(String... coordinates) {
        this.coordinates.addAll(Arrays.asList(coordinates));
        return this;
    }

    /// Add artifacts. The method handles [MvnRepoBom]s correctly.
    ///
    /// @param resources the resources
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup resolve(Stream<? extends MvnRepoResource> resources) {
        resources.forEach(r -> {
            if (r instanceof MvnRepoBom) {
                bom(r.coordinates());
            } else {
                resolve(r.coordinates());
            }
        });
        return this;
    }

    /// Failing to resolve the dependencies normally results in a
    /// [BuildException], because the requested artifacts are assumed
    /// to be required for the build.
    /// 
    /// By invoking this method the provider enters probe mode
    /// and returns an empty result stream instead of throwing an
    /// exception if the resolution fails.
    ///
    /// @return the mvn repo lookup
    ///
    public MvnRepoLookup probe() {
        probeMode = true;
        logger.atFine().log("Probe mode enabled for %s", this);
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
    /// @param request the requested resources
    /// @return the stream
    ///
    @Override
    protected <T extends Resource> Collection<T>
            doProvide(ResourceRequest<T> request) {
        if (request.accepts(MvnRepoDependencyType)) {
            return provideMvnDeps();
        }
        if (!request.accepts(MvnRepoLibraryJarFileType)) {
            return Collections.emptyList();
        }
        if (!request.isFor(MvnRepoLibraryJarFileType)) {
            @SuppressWarnings({ "unchecked", "PMD.AvoidDuplicateLiterals" })
            var result = (Collection<T>) context()
                .resources(this, of(MvnRepoLibraryJarFileType)).toList();
            return result;
        }
        try {
            return provideJars();
        } catch (ModelBuildingException e) {
            throw new BuildException().from(this).cause(e);
        } catch (DependencyResolutionException e) {
            if (probeMode) {
                return Collections.emptyList();
            }
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            throw new BuildException().from(this).cause(cause);
        }

    }

    private <T extends Resource> Collection<T> provideMvnDeps() {
        @SuppressWarnings("unchecked")
        var boms = (Stream<T>) this.boms.stream()
            .map(MvnRepoBom::of);
        @SuppressWarnings("unchecked")
        var deps = (Stream<T>) coordinates.stream()
            .map(MvnRepoDependency::of);
        return Stream.concat(boms, deps).toList();
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private <T extends Resource> Collection<T> provideJars()
            throws DependencyResolutionException, ModelBuildingException {
        @SuppressWarnings("PMD.CloseResource")
        var repoSystem = MavenContext.repositorySystem();
        var repoSession = MavenContext.repositorySession();

        // Create one synthetic CollectRequest
        var repos = new ArrayList<>(addedRepos);
        if (repos.isEmpty()) {
            repos.add(MavenContext.mavenCentral());
        }
        CollectRequest collectRequest
            = new CollectRequest().setRepositories(repos);

        // Add dependencies via their effective model
        coordinates.stream().parallel().map(c -> depsFromEffectiveModel(
            c, repoSystem, repoSession, repos)).forEach(deps -> {
                // collectRequest::addDependency is not thread safe
                synchronized (collectRequest) {
                    deps.forEach(collectRequest::addDependency);
                }
            });

        // Resolve dependencies - Resolver performs mediation
        logger.atFine().log("Resolving dependencies: %s",
            lazy(() -> collectRequest.getDependencies().stream()
                .map(Dependency::toString).collect(Collectors.joining(", "))));
        DependencyRequest dependencyRequest
            = new DependencyRequest(collectRequest, null);
        DependencyNode rootNode = repoSystem.resolveDependencies(repoSession,
            dependencyRequest).getRoot();
        logger.atFine().log("Dependency tree for %s:\n%s", name(),
            lazy(() -> buildTreeString(rootNode, 0, "", true)));
        List<DependencyNode> dependencyNodes = new ArrayList<>();
        rootNode.accept(new PreorderDependencyNodeConsumerVisitor(
            dependencyNodes::add));
        @SuppressWarnings("unchecked")
        var result = (Collection<T>) dependencyNodes.stream()
            .filter(d -> d.getArtifact() != null)
            .map(DependencyNode::getArtifact).map(a -> extraDownloads(
                repoSystem, repoSession, repos, a))
            .map(a -> ResourceFactory.create(MvnRepoLibraryJarFileType,
                a.toString(), a.getPath()))
            .toList();
        return result;
    }

    private Stream<Dependency> depsFromEffectiveModel(
            String coordinates, RepositorySystem repoSystem,
            RepositorySystemSession repoSession,
            List<RemoteRepository> repos) {
        // First build raw model
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId("model.group");
        model.setArtifactId("model.artifact");
        model.setVersion("0.0.0");
        model.setDescription(name());
        var depMgmt = new DependencyManagement();
        model.setDependencyManagement(depMgmt);

        // Build raw model from boms and coordinate
        for (String bom : boms) {
            var dep = DependencyConverter
                .convert(MvnRepoDependency.of(bom), "import");
            dep.setType("pom");
            depMgmt.addDependency(dep);
        }
        model.addDependency(DependencyConverter.convert(
            MvnRepoDependency.of(coordinates), "compile"));

        // Now build (derive) effective model and add its dependencies
        var buildingRequest = new DefaultModelBuildingRequest()
            .setRawModel(model).setProcessPlugins(false)
            .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
            .setModelResolver(
                new MvnModelResolver(repoSystem, repoSession, repos));
        try {
            var effectiveModel = new DefaultModelBuilderFactory()
                .newInstance().build(buildingRequest).getEffectiveModel();
            return effectiveModel.getDependencies().stream()
                .map(DependencyConverter::convert);
        } catch (ModelBuildingException e) {
            throw new BuildException().from(this).cause(e);
        }
    }

    private Artifact extraDownloads(
            RepositorySystem repoSystem, RepositorySystemSession repoSession,
            List<RemoteRepository> repos, Artifact artifact) {
        if (downloadSources) {
            downloadSourceJar(repoSystem, repoSession, repos, artifact);
        }
        if (downloadJavadoc) {
            downloadJavadocJar(repoSystem, repoSession, repos, artifact);
        }
        return artifact;
    }

    private void downloadSourceJar(RepositorySystem repoSystem,
            RepositorySystemSession repoSession,
            List<RemoteRepository> repos, Artifact jarArtifact) {
        Artifact sourcesArtifact
            = new SubArtifact(jarArtifact, "sources", "jar");
        ArtifactRequest sourcesRequest = new ArtifactRequest();
        sourcesRequest.setArtifact(sourcesArtifact);
        sourcesRequest.setRepositories(repos);
        try {
            repoSystem.resolveArtifact(repoSession, sourcesRequest);
        } catch (ArtifactResolutionException e) { // NOPMD
            // Ignore, sources are optional
        }
    }

    private void downloadJavadocJar(RepositorySystem repoSystem,
            RepositorySystemSession repoSession,
            List<RemoteRepository> repos, Artifact jarArtifact) {
        Artifact javadocArtifact
            = new SubArtifact(jarArtifact, "javadoc", "jar");
        ArtifactRequest sourcesRequest = new ArtifactRequest();
        sourcesRequest.setArtifact(javadocArtifact);
        sourcesRequest.setRepositories(repos);
        try {
            repoSystem.resolveArtifact(repoSession, sourcesRequest);
        } catch (ArtifactResolutionException e) { // NOPMD
            // Ignore, javadoc is optional
        }
    }

    private String buildTreeString(DependencyNode node, int indent,
            String prefix, boolean isLast) {
        @SuppressWarnings("PMD.ShortVariable")
        StringBuilder sb = new StringBuilder();
        var artifact = node.getArtifact();

        if (indent == 0) {
            sb.append("root\n");
        } else {
            sb.append(prefix).append(isLast ? "`-- " : "|-- ")
                .append(artifact != null ? artifact.toString() : "node")
                .append('\n');
        }

        var children = node.getChildren();
        String childPrefix
            = prefix + (indent == 0 ? "  " : isLast ? "    " : "|  ");

        for (int i = 0; i < children.size(); i++) {
            sb.append(buildTreeString(children.get(i), indent + 1, childPrefix,
                i == children.size() - 1));
        }
        return sb.toString();
    }
}
