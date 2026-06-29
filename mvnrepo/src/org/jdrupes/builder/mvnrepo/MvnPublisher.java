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
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.jdrupes.builder.api.BuildContext;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Generator;
import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.core.AbstractGenerator;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.JavadocJarFile;
import org.jdrupes.builder.java.LibraryJarFile;
import org.jdrupes.builder.java.SourcesJarFile;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

/// A [Generator] for Maven deployments in response to requests for
/// [MvnPublication] or [MvnInstallation]. It supports publishing
/// releases using the
/// [Publish Portal API](https://central.sonatype.org/publish/publish-portal-api/)
/// and publishing snapshots (and local installations) using the
/// "traditional" Maven approach (uploading the files individually,
/// including the appropriate `maven-metadata.xml` files).
///
/// The publisher requests the [PomFile] from the project and uses
/// the groupId, artfactId and version as specified in this file.
/// It also requests the [LibraryJarFile], the [SourcesJarFile] and
/// the [JavadocJarFile]. The latter two are optional for snapshot
/// releases.
///
/// Publishing requires a PGP/GPG secret key for signing the artifacts.
/// They can be set by the respective methods. However, it is assumed
/// that the credentials are usually made available as properties in
/// the build context.
/// 
/// Except for local installs, the publisher requires at least one
/// [MvnPublishingDestination] to publish to. If none is set, the
/// publisher adds an instance of [PortalPublisherDestination] for releases
/// and an instance of [MvnDeployDestination] with id "central"
/// for snapshots.
///
@SuppressWarnings({ "PMD.CouplingBetweenObjects", "PMD.ExcessiveImports",
    "PMD.GodClass" })
public class MvnPublisher extends AbstractGenerator {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private String signingKeyRing;
    private String signingKeyId;
    private String signingPassword;
    private JcaPGPContentSignerBuilder signerBuilder;
    private PGPPrivateKey privateKey;
    private PGPPublicKey publicKey;
    private Supplier<Path> artifactDirectory
        = () -> project().buildDirectory().resolve("publications/maven");
    private boolean keepSubArtifacts;
    private final List<MvnPublishingDestination> destinations
        = new ArrayList<>();

    /// Initializes a new Maven publication generator.
    ///
    /// @param project the project
    ///
    public MvnPublisher(Project project) {
        super(project);
    }

    /// Adds the given publishing destinations.
    ///
    /// @param destination the destinations
    /// @return the Maven publisher
    ///
    public MvnPublisher destination(MvnPublishingDestination... destination) {
        this.destinations.addAll(Arrays.asList(destination));
        return this;
    }

    /// Create and add a [MvnPublishingDestination] from the given arguments.
    ///
    /// @param type the type
    /// @param id the id
    /// @param uri the location
    /// @return the Maven publisher
    ///
    public MvnPublisher destination(MvnVersionType type, String id, URI uri) {
        destinations.add(new MvnDeployDestination(type)
            .repositoryUri(Objects.requireNonNull(uri))
            .id(Objects.requireNonNull(id)));
        return this;
    }

    /// Use the provided information to sign the artifacts. If no
    /// information is specified, the publisher will use the [BuildContext]
    /// to look up the properties `signing.secretKeyRingFile`,
    /// `signing.secretKey` and `signing.password`.
    ///
    /// The publisher retrieves the secret key from the key ring using the
    /// key ID. While this method makes signing in CI/CD pipelines
    /// more complex, it is considered best practice. 
    ///
    /// @param secretKeyRing the secret key ring
    /// @param keyId the key id
    /// @param password the password
    /// @return the mvn publisher
    ///
    public MvnPublisher signWith(String secretKeyRing, String keyId,
            String password) {
        this.signingKeyRing = Objects.requireNonNull(secretKeyRing);
        this.signingKeyId = Objects.requireNonNull(keyId);
        this.signingPassword = Objects.requireNonNull(password);
        return this;
    }

    /// Keep generated sub artifacts (checksums, signatures).
    ///
    /// @return the mvn publication generator
    ///
    public MvnPublisher keepSubArtifacts() {
        keepSubArtifacts = true;
        return this;
    }

    /// Returns the directory where additional artifacts are created.
    /// Defaults to sub directory `publications/maven` in the project's
    /// build directory (see [Project#buildDirectory]).
    ///
    /// @return the directory
    ///
    public Path artifactDirectory() {
        return artifactDirectory.get();
    }

    /// Sets the directory where additional artifacts are created.
    /// The [Path] is resolved against the project's build directory
    /// (see [Project#buildDirectory]). If `destination` is `null`,
    /// the additional artifacts are created in the directory where
    /// the base artifact is found.
    ///
    /// @param directory the new directory
    /// @return the maven publication generator
    ///
    public MvnPublisher artifactDirectory(Path directory) {
        if (directory == null) {
            this.artifactDirectory = () -> null;
            return this;
        }
        this.artifactDirectory
            = () -> project().buildDirectory().resolve(directory);
        return this;
    }

    /// Sets the directory where additional artifacts are created.
    /// If the [Supplier] returns `null`, the additional artifacts
    /// are created in the directory where the base artifact is found.
    ///
    /// @param directory the new directory
    /// @return the maven publication generator
    ///
    public MvnPublisher artifactDirectory(Supplier<Path> directory) {
        this.artifactDirectory = directory;
        return this;
    }

    @Override
    protected <T extends Resource> Collection<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.accepts(MvnPublicationType)
            && !requested.accepts(MvnInstallationType)) {
            return Collections.emptyList();
        }
        if (requested.accepts(MvnPublicationType) && destinations.isEmpty()) {
            destination(new PortalPublisherDestination(),
                new MvnDeployDestination(
                    MvnVersionType.SNAPSHOT).id("central"));
        }
        PomFile pomResource = resourceCheck(project()
            .resources(of(PomFileType).using(Supply)), "POM file");
        if (pomResource == null) {
            return Collections.emptyList();
        }
        var jarResource = resourceCheck(project()
            .resources(of(LibraryJarFileType).using(Supply)), "jar file");
        if (jarResource == null) {
            return Collections.emptyList();
        }
        var srcsIter = project()
            .resources(of(SourcesJarFileType).using(Supply)).iterator();
        SourcesJarFile srcsFile = null;
        if (srcsIter.hasNext()) {
            srcsFile = srcsIter.next();
            if (srcsIter.hasNext()) {
                logger.atSevere()
                    .log("More than one sources jar resources found.");
                return Collections.emptyList();
            }
        }
        var jdIter = project().resources(of(JavadocJarFileType).using(Supply))
            .iterator();
        JavadocJarFile jdFile = null;
        if (jdIter.hasNext()) {
            jdFile = jdIter.next();
            if (jdIter.hasNext()) {
                logger.atSevere()
                    .log("More than one javadoc jar resources found.");
                return Collections.emptyList();
            }
        }

        // Deploy what we've found
        @SuppressWarnings("unchecked")
        var result = (Collection<T>) publish(
            pomResource, jarResource, srcsFile, jdFile,
            requested.accepts(MvnInstallationType));
        return result;
    }

    private <T extends Resource> T resourceCheck(Stream<T> resources,
            String name) {
        var iter = resources.iterator();
        if (!iter.hasNext()) {
            logger.atSevere().log("No %s resource available", name);
            return null;
        }
        var result = iter.next();
        if (iter.hasNext()) {
            logger.atSevere().log("More than one %s resource found.", name);
            return null;
        }
        return result;
    }

    private record Deployable(Artifact artifact, boolean temporary) {
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private Collection<?> publish(PomFile pomResource,
            LibraryJarFile jarResource, SourcesJarFile srcsJar,
            JavadocJarFile javadocJar, boolean installOnly) {
        Artifact mainArtifact;
        try {
            mainArtifact = mainArtifact(pomResource);
        } catch (ModelBuildingException e) {
            throw new BuildException().from(this).cause(e);
        }
        if (artifactDirectory() != null) {
            artifactDirectory().toFile().mkdirs();
        }
        List<Deployable> toDeploy = new ArrayList<>();
        addWithGenerated(toDeploy, new SubArtifact(mainArtifact, "", "pom",
            pomResource.path().toFile()));
        addWithGenerated(toDeploy, new SubArtifact(mainArtifact, "", "jar",
            jarResource.path().toFile()));
        if (srcsJar != null) {
            addWithGenerated(toDeploy, new SubArtifact(mainArtifact, "sources",
                "jar", srcsJar.path().toFile()));
        }
        if (javadocJar != null) {
            addWithGenerated(toDeploy, new SubArtifact(mainArtifact, "javadoc",
                "jar", javadocJar.path().toFile()));
        }

        try {
            if (installOnly) {
                install(toDeploy);
                return List.of(MvnInstallation.of(String.format("%s:%s:%s",
                    mainArtifact.getGroupId(), mainArtifact.getArtifactId(),
                    mainArtifact.getVersion())));
            }
            destinations.stream().parallel().forEach(destination -> {
                if (mainArtifact.isSnapshot()
                    ? !destination.accepts(MvnVersionType.SNAPSHOT)
                    : !destination.accepts(MvnVersionType.RELEASE)) {
                    return;
                }
                destination.publish(context(), this, mainArtifact,
                    toDeploy.stream().map(d -> d.artifact).toList());
            });
            return List.of(MvnPublication.of(String.format("%s:%s:%s",
                mainArtifact.getGroupId(), mainArtifact.getArtifactId(),
                mainArtifact.getVersion())));
        } finally {
            if (!keepSubArtifacts) {
                toDeploy.stream().filter(Deployable::temporary).forEach(d -> {
                    d.artifact().getPath().toFile().delete();
                });
            }
        }
    }

    private Artifact mainArtifact(PomFile pomResource)
            throws ModelBuildingException {
        var pomFile = pomResource.path().toFile();
        var buildingRequest = new DefaultModelBuildingRequest()
            .setPomFile(pomFile).setProcessPlugins(false)
            .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        var model = new DefaultModelBuilderFactory().newInstance()
            .build(buildingRequest).getEffectiveModel();
        return new DefaultArtifact(model.getGroupId(), model.getArtifactId(),
            "jar", model.getVersion());
    }

    private void addWithGenerated(List<Deployable> toDeploy,
            Artifact artifact) {
        // Add main artifact
        toDeploy.add(new Deployable(artifact, false));

        // Generate .md5 and .sha1 checksum files
        var artifactFile = artifact.getPath();
        try {
            // Add signature as yet another artifact
            var sigPath = signResource(artifactFile);
            toDeploy.add(new Deployable(new SubArtifact(artifact, "*", "*.asc",
                sigPath.toFile()), true));
        } catch (IOException | PGPException e) {
            throw new BuildException().from(this).cause(e);
        }
    }

    private Path destinationPath(Path base, String fileName) {
        var dir = artifactDirectory();
        if (dir == null) {
            base.resolveSibling(fileName);
        }
        return dir.resolve(fileName);
    }

    private void initSigning()
            throws FileNotFoundException, IOException, PGPException {
        if (signerBuilder != null) {
            return;
        }
        var keyRingFileName = Optional.ofNullable(signingKeyRing).orElse(
            project().context().property("signing.secretKeyRingFile", null));
        var keyId = Optional.ofNullable(signingKeyId)
            .orElse(project().context().property("signing.keyId", null));
        var passphrase = Optional.ofNullable(signingPassword)
            .or(() -> Optional.ofNullable(
                project().context().property("signing.password", null)))
            .map(String::toCharArray).orElse(null);
        if (keyRingFileName == null || keyId == null || passphrase == null) {
            logger.atWarning()
                .log("Cannot sign artifacts: properties not set.");
            return;
        }
        Security.addProvider(new BouncyCastleProvider());
        var secretKeyRingCollection = new PGPSecretKeyRingCollection(
            PGPUtil.getDecoderStream(
                Files.newInputStream(Path.of(keyRingFileName))),
            new JcaKeyFingerprintCalculator());
        var secretKey = secretKeyRingCollection
            .getSecretKey(Long.parseUnsignedLong(keyId, 16));
        publicKey = secretKey.getPublicKey();
        privateKey = secretKey.extractPrivateKey(
            new JcePBESecretKeyDecryptorBuilder().setProvider("BC")
                .build(passphrase));
        signerBuilder = new JcaPGPContentSignerBuilder(
            publicKey.getAlgorithm(), PGPUtil.SHA256).setProvider("BC");
    }

    private Path signResource(Path resource)
            throws PGPException, IOException {
        initSigning();
        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
            signerBuilder, publicKey);
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
        var sigPath = destinationPath(resource,
            resource.getFileName() + ".asc");
        try (InputStream fileInput = new BufferedInputStream(
            Files.newInputStream(resource));
                OutputStream sigOut
                    = new ArmoredOutputStream(Files.newOutputStream(sigPath))) {
            byte[] buffer = new byte[8192];
            while (true) {
                int read = fileInput.read(buffer);
                if (read < 0) {
                    break;
                }
                signatureGenerator.update(buffer, 0, read);
            }
            PGPSignature signature = signatureGenerator.generate();
            signature.encode(sigOut);
        }
        return sigPath;
    }

    private void install(List<Deployable> toDeploy) {
        var session = new DefaultRepositorySystemSession(
            MavenContext.repositorySession());
        var installReq = new InstallRequest();
        toDeploy.stream().map(d -> d.artifact).forEach(installReq::addArtifact);
        try {
            MavenContext.repositorySystem().install(session, installReq);
        } catch (InstallationException e) {
            throw new BuildException().from(this).cause(e);
        }
    }

}
