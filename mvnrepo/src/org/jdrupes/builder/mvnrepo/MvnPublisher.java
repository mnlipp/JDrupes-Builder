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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
import org.bouncycastle.util.encoders.Base64;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jdrupes.builder.api.BuildContext;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Generator;
import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.java.JavadocJarFile;
import org.jdrupes.builder.java.LibraryJarFile;
import org.jdrupes.builder.java.SourcesJarFile;
import static org.jdrupes.builder.mvnrepo.MvnProperties.ArtifactId;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

/// A [Generator] for maven deployments in response to requests for
/// [MvnPublication] It supports publishing releases using the
/// [Publish Portal API](https://central.sonatype.org/publish/publish-portal-api/)
/// and publishing snapshots using the "traditional" maven approach
/// (uploading the files, including the appropriate `maven-metadata.xml`
/// files).
///
/// The publisher requests the [PomFile] from the project and uses
/// the groupId, artfactId and version as specified in this file.
/// It also requests the [LibraryJarFile], the [SourcesJarFile] and
/// the [JavadocJarFile]. The latter two are optional for snapshot
/// releases.
///
/// Publishing requires credentials for the maven repository and a
/// PGP/GPG secret key for signing the artifacts. They can be set by
/// the respective methods. However, it is assumed that the credentials
/// are usually made available as properties in the build context.
///
@SuppressWarnings({ "PMD.CouplingBetweenObjects", "PMD.ExcessiveImports",
    "PMD.GodClass", "PMD.TooManyMethods" })
public class MvnPublisher extends AbstractGenerator {

    private URI uploadUri;
    private URI snapshotUri;
    private String repoUser;
    private String repoPass;
    private String signingKeyRing;
    private String signingKeyId;
    private String signingPassword;
    private JcaPGPContentSignerBuilder signerBuilder;
    private PGPPrivateKey privateKey;
    private PGPPublicKey publicKey;
    private Supplier<Path> artifactDirectory
        = () -> project().buildDirectory().resolve("publications/maven");
    private boolean keepSubArtifacts;
    private boolean publishAutomatically;

    /// Creates a new Maven publication generator.
    ///
    /// @param project the project
    ///
    public MvnPublisher(Project project) {
        super(project);
        uploadUri = URI
            .create("https://central.sonatype.com/api/v1/publisher/upload");
        snapshotUri = URI
            .create("https://central.sonatype.com/repository/maven-snapshots/");
    }

    /// Sets the upload URI.
    ///
    /// @param uri the repository URI
    /// @return the maven publication generator
    ///
    public MvnPublisher uploadUri(URI uri) {
        this.uploadUri = uri;
        return this;
    }

    /// Returns the upload URI. Defaults to 
    /// `https://central.sonatype.com/api/v1/publisher/upload`.
    ///
    /// @return the uri
    ///
    public URI uploadUri() {
        return uploadUri;
    }

    /// Sets the Maven snapshot repository URI.
    ///
    /// @param uri the snapshot repository URI
    /// @return the maven publication generator
    ///
    public MvnPublisher snapshotRepository(URI uri) {
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

    /// Sets the Maven repository credentials. If not specified, the
    /// publisher looks for properties `mvnrepo.user` and
    /// `mvnrepo.password` in the properties provided by the [BuildContext].
    ///
    /// @param user the username
    /// @param pass the password
    /// @return the maven publication generator
    ///
    public MvnPublisher credentials(String user, String pass) {
        this.repoUser = user;
        this.repoPass = pass;
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
        this.signingKeyRing = secretKeyRing;
        this.signingKeyId = keyId;
        this.signingPassword = password;
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

    /// Publish the release automatically.
    ///
    /// @return the mvn publisher
    ///
    public MvnPublisher publishAutomatically() {
        publishAutomatically = true;
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
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.accepts(MvnPublicationType)) {
            return Stream.empty();
        }
        PomFile pomResource = resourceCheck(project()
            .resources(of(PomFile.class).using(Supply)), "POM file");
        if (pomResource == null) {
            return Stream.empty();
        }
        var jarResource = resourceCheck(project()
            .resources(of(LibraryJarFile.class).using(Supply)), "jar file");
        if (jarResource == null) {
            return Stream.empty();
        }
        var srcsIter = project()
            .resources(of(SourcesJarFile.class).using(Supply)).iterator();
        SourcesJarFile srcsFile = null;
        if (srcsIter.hasNext()) {
            srcsFile = srcsIter.next();
            if (srcsIter.hasNext()) {
                log.severe(() -> "More than one sources jar resources found.");
                return Stream.empty();
            }
        }
        var jdIter = project().resources(of(JavadocJarFile.class).using(Supply))
            .iterator();
        JavadocJarFile jdFile = null;
        if (jdIter.hasNext()) {
            jdFile = jdIter.next();
            if (jdIter.hasNext()) {
                log.severe(() -> "More than one javadoc jar resources found.");
                return Stream.empty();
            }
        }

        // Deploy what we've found
        @SuppressWarnings("unchecked")
        var result = (Stream<T>) deploy(pomResource, jarResource, srcsFile,
            jdFile);
        return result;
    }

    private <T extends Resource> T resourceCheck(Stream<T> resources,
            String name) {
        var iter = resources.iterator();
        if (!iter.hasNext()) {
            log.severe(() -> "No " + name + " resource available.");
            return null;
        }
        var result = iter.next();
        if (iter.hasNext()) {
            log.severe(() -> "More than one " + name + " resource found.");
            return null;
        }
        return result;
    }

    private record Deployable(Artifact artifact, boolean temporary) {
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private Stream<?> deploy(PomFile pomResource, LibraryJarFile jarResource,
            SourcesJarFile srcsJar, JavadocJarFile javadocJar) {
        Artifact mainArtifact;
        try {
            mainArtifact = mainArtifact(pomResource);
        } catch (ModelBuildingException e) {
            throw new BuildException(
                "Cannot build model from POM: " + e.getMessage(), e);
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
            if (mainArtifact.isSnapshot()) {
                deploySnapshot(toDeploy);
            } else {
                deployRelease(mainArtifact, toDeploy);
            }
        } catch (DeploymentException e) {
            throw new BuildException(
                "Deployment failed for " + mainArtifact, e);
        } finally {
            if (!keepSubArtifacts) {
                toDeploy.stream().filter(Deployable::temporary).forEach(d -> {
                    d.artifact().getFile().delete();
                });
            }
        }
        return Stream.of(project().newResource(MvnPublicationType,
            mainArtifact.getGroupId() + ":" + mainArtifact.getArtifactId()
                + ":" + mainArtifact.getVersion()));
    }

    private Artifact mainArtifact(PomFile pomResource)
            throws ModelBuildingException {
        var repoSystem = MvnRepoLookup.rootContext().repositorySystem();
        var repoSession = MvnRepoLookup.rootContext().repositorySystemSession();
        var repos
            = new ArrayList<>(MvnRepoLookup.rootContext().remoteRepositories());
        if (snapshotUri != null) {
            repos.add(createSnapshotRepository());
        }
        var pomFile = pomResource.path().toFile();
        var buildingRequest = new DefaultModelBuildingRequest()
            .setPomFile(pomFile).setProcessPlugins(false)
            .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
            .setModelResolver(
                new MvnModelResolver(repoSystem, repoSession, repos));
        var model = new DefaultModelBuilderFactory().newInstance()
            .build(buildingRequest).getEffectiveModel();
        return new DefaultArtifact(model.getGroupId(), model.getArtifactId(),
            "jar", model.getVersion());
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

    private void addWithGenerated(List<Deployable> toDeploy,
            Artifact artifact) {
        // Add main artifact
        toDeploy.add(new Deployable(artifact, false));

        // Generate .md5 and .sha1 checksum files
        var artifactFile = artifact.getFile().toPath();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            try (var fis = Files.newInputStream(artifactFile)) {
                byte[] buffer = new byte[8192];
                while (true) {
                    int read = fis.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    md5.update(buffer, 0, read);
                    sha1.update(buffer, 0, read);
                }
            }
            var fileName = artifactFile.getFileName().toString();

            // Handle generated md5
            var md5Path = destinationPath(artifactFile, fileName + ".md5");
            Files.writeString(md5Path, toHex(md5.digest()));
            toDeploy.add(new Deployable(new SubArtifact(artifact, "*", "*.md5",
                md5Path.toFile()), true));

            // Handle generated sha1
            var sha1Path = destinationPath(artifactFile, fileName + ".sha1");
            Files.writeString(sha1Path, toHex(sha1.digest()));
            toDeploy.add(new Deployable(new SubArtifact(artifact, "*", "*.sha1",
                sha1Path.toFile()), true));

            // Add signature as yet another artifact
            var sigPath = signResource(artifactFile);
            toDeploy.add(new Deployable(new SubArtifact(artifact, "*", "*.asc",
                sigPath.toFile()), true));
        } catch (NoSuchAlgorithmException | IOException | PGPException e) {
            throw new BuildException(e);
        }
    }

    private Path destinationPath(Path base, String fileName) {
        var dir = artifactDirectory();
        if (dir == null) {
            base.resolveSibling(fileName);
        }
        return dir.resolve(fileName);
    }

    private static String toHex(byte[] bytes) {
        char[] hexDigits = "0123456789abcdef".toCharArray();
        char[] result = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; i++) {
            int unsigned = bytes[i] & 0xFF;
            result[i * 2] = hexDigits[unsigned >>> 4];
            result[i * 2 + 1] = hexDigits[unsigned & 0x0F];
        }
        return new String(result);
    }

    private void initSigning()
            throws FileNotFoundException, IOException, PGPException {
        if (signerBuilder != null) {
            return;
        }
        var keyRingFileName = Optional.ofNullable(signingKeyRing)
            .orElse(project().context().property("signing.secretKeyRingFile"));
        var keyId = Optional.ofNullable(signingKeyId)
            .orElse(project().context().property("signing.keyId"));
        var passphrase = Optional.ofNullable(signingPassword)
            .orElse(project().context().property("signing.password"))
            .toCharArray();
        if (keyRingFileName == null || keyId == null || passphrase == null) {
            log.warning(() -> "Cannot sign artifacts: properties not set.");
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

    private void deploySnapshot(List<Deployable> toDeploy)
            throws DeploymentException {
        // Now deploy everything
        @SuppressWarnings("PMD.CloseResource")
        var context = MvnRepoLookup.rootContext();
        var session = new DefaultRepositorySystemSession(
            context.repositorySystemSession());
        var startMsgLogged = new AtomicBoolean(false);
        session.setRepositoryListener(new AbstractRepositoryListener() {
            @Override
            public void artifactDeploying(RepositoryEvent event) {
                if (!startMsgLogged.getAndSet(true)) {
                    log.info(() -> "Start deploying artifacts...");
                }
            }

            @Override
            public void artifactDeployed(RepositoryEvent event) {
                if (!"jar".equals(event.getArtifact().getExtension())) {
                    return;
                }
                log.info(() -> "Deployed: " + event.getArtifact());
            }

            @Override
            public void metadataDeployed(RepositoryEvent event) {
                log.info(() -> "Deployed: " + event.getMetadata());
            }

        });
        var user = Optional.ofNullable(repoUser)
            .orElse(project().context().property("mvnrepo.user"));
        var password = Optional.ofNullable(repoPass)
            .orElse(project().context().property("mvnrepo.password"));
        var repo = new RemoteRepository.Builder("mine", "default",
            snapshotUri.toString())
                .setAuthentication(new AuthenticationBuilder()
                    .addUsername(user).addPassword(password).build())
                .build();
        var deployReq = new DeployRequest().setRepository(repo);
        toDeploy.stream().map(d -> d.artifact).forEach(deployReq::addArtifact);
        context.repositorySystem().deploy(session, deployReq);
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private void deployRelease(Artifact mainArtifact,
            List<Deployable> toDeploy) {
        // Create zip file with all artifacts for release, see
        // https://central.sonatype.org/publish/publish-portal-upload/
        var zipName = Optional.ofNullable(project().get(ArtifactId))
            .orElse(project().name()) + "-" + mainArtifact.getVersion()
            + "-release.zip";
        var zipPath = artifactDirectory().resolve(zipName);
        try {
            Path praefix = Path.of(mainArtifact.getGroupId().replace('.', '/'))
                .resolve(mainArtifact.getArtifactId())
                .resolve(mainArtifact.getVersion());
            try (ZipOutputStream zos
                = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                for (Deployable d : toDeploy) {
                    var artifact = d.artifact();
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    var entry = new ZipEntry(praefix.resolve(
                        artifact.getArtifactId() + "-" + artifact.getVersion()
                            + (artifact.getClassifier().isEmpty()
                                ? ""
                                : "-" + artifact.getClassifier())
                            + "." + artifact.getExtension())
                        .toString());
                    zos.putNextEntry(entry);
                    try (var fis = Files.newInputStream(
                        artifact.getFile().toPath())) {
                        fis.transferTo(zos);
                    }
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new BuildException(
                "Failed to create release zip: " + e.getMessage(), e);
        }

        try (var client = HttpClient.newHttpClient()) {
            var boundary = "===" + System.currentTimeMillis() + "===";
            var user = Optional.ofNullable(repoUser)
                .orElse(project().context().property("mvnrepo.user"));
            var password = Optional.ofNullable(repoPass)
                .orElse(project().context().property("mvnrepo.password"));
            var token = new String(Base64.encode((user + ":" + password)
                .getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            var effectiveUri = uploadUri;
            if (publishAutomatically) {
                effectiveUri = addQueryParameter(
                    uploadUri, "publishingType", "AUTOMATIC");
            }
            HttpRequest request = HttpRequest.newBuilder().uri(effectiveUri)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type",
                    "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers
                    .ofInputStream(() -> getAsMultipart(zipPath, boundary)))
                .build();
            log.info(() -> "Uploading release bundle...");
            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
            log.finest(() -> "Upload response: " + response.body());
            if (response.statusCode() / 100 != 2) {
                throw new BuildException(
                    "Failed to upload release bundle: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new BuildException(
                "Failed to upload release bundle: " + e.getMessage(), e);
        } finally {
            if (!keepSubArtifacts) {
                zipPath.toFile().delete();
            }
        }
    }

    @SuppressWarnings("PMD.UseTryWithResources")
    private InputStream getAsMultipart(Path zipPath, String boundary) {
        // Use Piped streams for streaming multipart content
        var fromPipe = new PipedInputStream();

        // Write multipart content to pipe
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        OutputStream toPipe;
        try {
            toPipe = new PipedOutputStream(fromPipe);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        executor.submit(() -> {
            try (var mpOut = new BufferedOutputStream(toPipe)) {
                final String lineFeed = "\r\n";
                @SuppressWarnings("PMD.InefficientStringBuffering")
                StringBuilder intro = new StringBuilder(100)
                    .append("--").append(boundary).append(lineFeed)
                    .append("Content-Disposition: form-data; name=\"bundle\";"
                        + " filename=\"%s\"".formatted(zipPath.getFileName()))
                    .append(lineFeed)
                    .append("Content-Type: application/octet-stream")
                    .append(lineFeed).append(lineFeed);
                mpOut.write(
                    intro.toString().getBytes(StandardCharsets.US_ASCII));
                Files.newInputStream(zipPath).transferTo(mpOut);
                mpOut.write((lineFeed + "--" + boundary + "--")
                    .getBytes(StandardCharsets.US_ASCII));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                executor.close();
            }
        });
        return fromPipe;
    }

    private static URI addQueryParameter(URI uri, String key, String value) {
        String query = uri.getQuery();
        try {
            String newQueryParam
                = key + "=" + URLEncoder.encode(value, "UTF-8");
            String newQuery = (query == null || query.isEmpty()) ? newQueryParam
                : query + "&" + newQueryParam;

            // Build a new URI with the new query string
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(),
                newQuery, uri.getFragment());
        } catch (UnsupportedEncodingException | URISyntaxException e) {
            // UnsupportedEncodingException cannot happen, UTF-8 is standard.
            // URISyntaxException cannot happen when starting with a valid URI
            throw new IllegalArgumentException(e);
        }
    }
}
