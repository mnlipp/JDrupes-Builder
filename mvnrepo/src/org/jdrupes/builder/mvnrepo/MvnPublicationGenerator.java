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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceRequest.requestFor;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.java.JarGenerator;
import org.jdrupes.builder.java.JavadocJarFile;
import org.jdrupes.builder.java.LibraryJarFile;
import org.jdrupes.builder.java.SourcesJarFile;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

@SuppressWarnings("PMD.CouplingBetweenObjects")
public class MvnPublicationGenerator extends AbstractGenerator {

    private URI repositoryUri;
    private URI snapshotUri;
    private String repoUser;
    private String repoPass;
    private JcaPGPContentSignerBuilder signerBuilder;
    private PGPPrivateKey privateKey;
    private PGPPublicKey publicKey;
    private Supplier<Path> artifactDirectory
        = () -> project().buildDirectory().resolve("publications/maven");

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
    public MvnPublicationGenerator artifactDirectory(Path directory) {
        if (directory == null) {
            this.artifactDirectory = () -> null;
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
    public MvnPublicationGenerator artifactDirectory(Supplier<Path> directory) {
        this.artifactDirectory = directory;
        return this;
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

    @Override
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.includes(MvnPublicationType)) {
            return Stream.empty();
        }
        PomFile pomResource = resourceCheck(project().supplied(
            requestFor(PomFile.class)), "POM file");
        if (pomResource == null) {
            return Stream.empty();
        }
        var jarResource = resourceCheck(project().getFrom(project()
            .providers(Expose, Forward), requestFor(LibraryJarFile.class)),
            "jar file");
        if (jarResource == null) {
            return Stream.empty();
        }
        var srcsIter = project().supplied(requestFor(SourcesJarFile.class))
            .iterator();
        SourcesJarFile srcsFile = null;
        if (srcsIter.hasNext()) {
            srcsFile = srcsIter.next();
            if (srcsIter.hasNext()) {
                log.severe(() -> "More than one sources jar resources found.");
                return Stream.empty();
            }
        }
        var jdIter = project().supplied(requestFor(JavadocJarFile.class))
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
        try {
            @SuppressWarnings("unchecked")
            var result = (Stream<T>) deploy(pomResource, jarResource, srcsFile,
                jdFile);
            return result;
        } catch (ModelBuildingException | DeploymentException e) {
            throw new BuildException(e);
        }
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private Stream<?> deploy(PomFile pomResource, LibraryJarFile jarResource,
            SourcesJarFile srcsFile, JavadocJarFile jdFile)
            throws ModelBuildingException, DeploymentException {
        var mainArtifact = mainArtifact(pomResource);
        var isSnapshot = mainArtifact.isSnapshot();
        var repo = new RemoteRepository.Builder(
            "mine", "default",
            (isSnapshot ? snapshotUri : repositoryUri)
                .toString()).setAuthentication(new AuthenticationBuilder()
                    .addUsername(repoUser).addPassword(repoPass)
                    .build()).build();
        var deployReq = new DeployRequest().setRepository(repo);
        if (artifactDirectory() != null) {
            artifactDirectory().toFile().mkdirs();
        }
        addArtifact(deployReq, new SubArtifact(mainArtifact, "", "pom"),
            pomResource);
        addArtifact(deployReq, new SubArtifact(mainArtifact, "", "jar"),
            jarResource);
        if (srcsFile != null) {
            addArtifact(deployReq,
                new SubArtifact(mainArtifact, "sources", "jar"), srcsFile);
        }
        if (jdFile != null) {
            addArtifact(deployReq,
                new SubArtifact(mainArtifact, "javadoc", "jar"), jdFile);
        }

        // Now deploy everything
        @SuppressWarnings("PMD.CloseResource")
        var context = MvnRepoLookup.rootContext();
        // TODO disable temporarily
        var result = context.repositorySystem().deploy(
            context.repositorySystemSession(), deployReq);
        return Stream.of(project().newResource(MvnPublicationType,
            mainArtifact.getGroupId() + ":" + mainArtifact.getArtifactId()
                + ":" + mainArtifact.getVersion()));
    }

    private Artifact mainArtifact(PomFile pomResource)
            throws ModelBuildingException {
        var pomFile = pomResource.path().toFile();
        var req = new DefaultModelBuildingRequest().setPomFile(pomFile);
        var model = new DefaultModelBuilderFactory().newInstance()
            .build(req).getEffectiveModel();
        return new DefaultArtifact(model.getGroupId(), model.getArtifactId(),
            "jar", model.getVersion());
    }

    private void addArtifact(DeployRequest deployReq, Artifact artifact,
            FileResource resource) {
        deployReq.addArtifact(artifact.setFile(resource.path().toFile()));

        // Generate .md5 and .sha1 checksum files
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            try (var fis = Files.newInputStream(resource.path())) {
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
            var fileName = resource.path().getFileName().toString();

            // Handle generated md5
            var md5Hex = toHex(md5.digest()) + "  " + fileName + "\n";
            var md5Path = destinationPath(resource, fileName + ".md5");
            Files.writeString(md5Path, md5Hex);
            deployReq.addArtifact(new SubArtifact(artifact, "*", "*.md5")
                .setFile(md5Path.toFile()));

            // Handle generated sha1
            String sha1Hex = toHex(sha1.digest()) + "  " + fileName + "\n";
            var sha1Path = destinationPath(resource, fileName + ".sha1");
            Files.writeString(sha1Path, sha1Hex);
            deployReq.addArtifact(new SubArtifact(artifact, "*", "*.sha1")
                .setFile(sha1Path.toFile()));

            // Add signature as yet another artifact
            var sigPath = signResource(resource);
            deployReq.addArtifact(new SubArtifact(artifact, "*", "*.asc")
                .setFile(sigPath.toFile()));
        } catch (NoSuchAlgorithmException | IOException | PGPException e) {
            throw new BuildException(e);
        }
    }

    private Path destinationPath(FileResource base, String fileName) {
        var dir = artifactDirectory();
        if (dir == null) {
            base.path().resolveSibling(fileName);
        }
        return dir.resolve(fileName);
    }

    public static String toHex(byte[] bytes) {
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
        var keyRingFileName
            = project().context().property("signing.secretKeyRingFile");
        var keyId = project().context().property("signing.keyId");
        var passphrase
            = project().context().property("signing.password").toCharArray();
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

    private Path signResource(FileResource resource)
            throws PGPException, IOException {
        initSigning();
        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
            signerBuilder, publicKey);
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
        var sigPath = destinationPath(resource,
            resource.path().getFileName() + ".asc");
        try (InputStream fileInput = new BufferedInputStream(
            Files.newInputStream(resource.path()));
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
}
