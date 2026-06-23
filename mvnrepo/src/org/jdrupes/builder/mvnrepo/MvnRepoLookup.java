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
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
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
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
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
///     Note that the result also includes the [MvnRepoBom]s.
///
///  2. The resources of type [MvnRepoLibraryJarFile] that result from
///     resolving the artifacts to be resolved.
///
@SuppressWarnings({ "PMD.CouplingBetweenObjects" })
public class MvnRepoLookup extends AbstractProvider {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private static MavenContext mavenContext;
    private final List<String> coordinates = new ArrayList<>();
    private final List<String> boms = new ArrayList<>();
    private boolean downloadSources = true;
    private boolean downloadJavadoc = true;
    private URI snapshotUri;
    private boolean probeMode;

    /// Initializes a new Maven repository lookup.
    ///
    public MvnRepoLookup() {
        // Make javadoc happy.
    }

    /// Returns the (singleton) Maven context.
    ///
    /// @return the Maven context
    ///
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    public static synchronized MavenContext mavenContext() {
        if (mavenContext != null) {
            return mavenContext;
        }

        // Settings
        SettingsBuildingRequest settingsRequest
            = new DefaultSettingsBuildingRequest()
                .setUserSettingsFile(new File(System.getProperty("user.home"),
                    ".m2/settings.xml"));
        SettingsBuilder settingsBuilder
            = new DefaultSettingsBuilderFactory().newInstance();
        SettingsBuildingResult settingsResult;
        try {
            settingsResult = settingsBuilder.build(settingsRequest);
        } catch (SettingsBuildingException e) {
            throw new BuildException().cause(e);
        }
        var settings = settingsResult.getEffectiveSettings();

        // Repository system
        @SuppressWarnings("PMD.CloseResource")
        var repoSystem = new RepositorySystemSupplier().get();

        // Repository system session
        String localRepoPath = settings.getLocalRepository() != null
            ? settings.getLocalRepository()
            : System.getProperty("user.home") + "/.m2/repository";
        @SuppressWarnings("PMD.CloseResource")
        var session = new SessionBuilderSupplier(repoSystem).get()
            .withLocalRepositoryBaseDirectories(Path.of(localRepoPath))
            .build();

        // Combine
        mavenContext = new MavenContext(settings, repoSystem, session,
            remoteRepositories(settings));
        return mavenContext;
    }

    private static List<RemoteRepository>
            remoteRepositories(Settings settings) {
        List<RemoteRepository> repos = new ArrayList<>();

        Map<String, Profile> profiles = settings.getProfiles().stream()
            .collect(Collectors.toMap(Profile::getId, Function.identity()));

        for (String profileId : settings.getActiveProfiles()) {
            Profile profile = profiles.get(profileId);
            if (profile == null) {
                continue;
            }
            for (Repository repo : profile.getRepositories()) {
                repos.add(new RemoteRepository.Builder(repo.getId(),
                    "default", repo.getUrl()).build());
            }
        }

        if (repos.isEmpty()) {
            repos.add(new RemoteRepository.Builder("central", "default",
                "https://repo.maven.apache.org/maven2").build());
        }

        return repos;
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
    /// @return the snapshot repository
    ///
    public URI snapshotRepository() {
        return snapshotUri;
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

    /// Add artifacts, specified by their coordinates
    /// (`groupId:artifactId:version`) as resources.
    ///
    /// @param coordinates the coordinates
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

    private <T extends Resource> Collection<T> provideJars()
            throws DependencyResolutionException, ModelBuildingException {
        @SuppressWarnings("PMD.CloseResource")
        var repoSystem = mavenContext().repositorySystem();
        var repoSession = mavenContext().repositorySession();
        var repos = new ArrayList<>(mavenContext().remoteRepositories());
        if (snapshotUri != null) {
            repos.add(createSnapshotRepository());
        }

        // Create an effective model. This make sure that BOMs are
        // handled correctly.
        Model model = getEffectiveModel(repoSystem, repoSession,
            repos);

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
        List<DependencyNode> dependencyNodes = new ArrayList<>();
        rootNode.accept(new PreorderDependencyNodeConsumerVisitor(
            dependencyNodes::add));
        @SuppressWarnings("unchecked")
        var result = (Collection<T>) dependencyNodes.stream()
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
            })
            .map(a -> ResourceFactory.create(MvnRepoLibraryJarFileType,
                a.toString(), a.getPath()))
            .toList();
        return result;
    }

    private Model getEffectiveModel(RepositorySystem repoSystem,
            RepositorySystemSession repoSession, List<RemoteRepository> repos)
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
            var mvnResource = MvnRepoDependency.of(c);
            var dep = DependencyConverter.convert(mvnResource, "import");
            dep.setType("pom");
            depMgmt.addDependency(dep);
        });
        coordinates.forEach(c -> {
            var mvnResource = MvnRepoDependency.of(c);
            model.addDependency(
                DependencyConverter.convert(mvnResource, "compile"));
        });

        // Now build (derive) effective model
        var buildingRequest = new DefaultModelBuildingRequest()
            .setRawModel(model).setProcessPlugins(false)
            .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
            .setModelResolver(
                new MvnModelResolver(repoSystem, repoSession, repos));
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
        sourcesRequest.setRepositories(mavenContext().remoteRepositories());
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
        sourcesRequest.setRepositories(mavenContext().remoteRepositories());
        try {
            repoSystem.resolveArtifact(repoSession, sourcesRequest);
        } catch (ArtifactResolutionException e) { // NOPMD
            // Ignore, javadoc is optional
        }
    }
}
