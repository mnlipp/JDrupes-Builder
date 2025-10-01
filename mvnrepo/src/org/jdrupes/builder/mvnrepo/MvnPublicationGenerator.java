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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.Security;
import java.util.stream.Stream;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jdrupes.builder.api.BuildException;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceRequest.requestFor;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.java.JavadocJarFile;
import org.jdrupes.builder.java.LibraryJarFile;
import org.jdrupes.builder.java.SourcesJarFile;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.bcpg.ArmoredOutputStream;

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

        // Get the coordinates from the model
        try {
            return (Stream<T>) deploy(pomResource, jarResource, srcsFile,
                jdFile);
        } catch (ModelBuildingException | DeploymentException e) {
            throw new BuildException(e);
        }
    }

    private Stream<?> deploy(PomFile pomResource, LibraryJarFile jarResource,
            SourcesJarFile srcsFile, JavadocJarFile jdFile)
            throws ModelBuildingException, DeploymentException {
        var pomFile = pomResource.path().toFile();
        var req = new DefaultModelBuildingRequest().setPomFile(pomFile);
        var model = new DefaultModelBuilderFactory().newInstance()
            .build(req).getEffectiveModel();
        var mainArtifact = new DefaultArtifact(model.getGroupId(),
            model.getArtifactId(), "", model.getVersion());
        var isSnapshot = mainArtifact.isSnapshot();
        var repo = new RemoteRepository.Builder(
            "mine", "default",
            (isSnapshot ? snapshotUri : repositoryUri)
                .toString()).setAuthentication(new AuthenticationBuilder()
                    .addUsername(repoUser).addPassword(repoPass)
                    .build()).build();
        var deployReq = new DeployRequest().setRepository(repo)
            .addArtifact(new SubArtifact(mainArtifact, "", "pom")
                .setFile(pomFile))
            .addArtifact(new SubArtifact(mainArtifact, "", "jar")
                .setFile(jarResource.path().toFile()));
        if (srcsFile != null) {
            deployReq.addArtifact(new SubArtifact(mainArtifact,
                "sources", "jar").setFile(srcsFile.path().toFile()));
        }
        if (jdFile != null) {
            deployReq.addArtifact(new SubArtifact(mainArtifact,
                "javadoc", "jar").setFile(jdFile.path().toFile()));
        }

        @SuppressWarnings("PMD.CloseResource")
        var context = MvnRepoLookup.rootContext();
        var result = context.repositorySystem().deploy(
            context.repositorySystemSession(), deployReq);
        return Stream
            .of(project().newResource(MvnPublicationType,
                model.getGroupId() + ":" + model.getArtifactId() + ":"
                    + model.getVersion()));
    }

    public static void createDetachedSignature(
            File inputFile, File signatureFile,
            PGPSecretKey secretKey, char[] passphrase) throws Exception {
        Security.addProvider(
            new org.bouncycastle.jce.provider.BouncyCastleProvider());
        PGPPrivateKey privateKey = secretKey.extractPrivateKey(
            new JcePBESecretKeyDecryptorBuilder().setProvider("BC")
                .build(passphrase));
        JcaPGPContentSignerBuilder signerBuilder
            = new JcaPGPContentSignerBuilder(
                secretKey.getPublicKey().getAlgorithm(), PGPUtil.SHA256)
                    .setProvider("BC");
        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
            signerBuilder, secretKey.getPublicKey());
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);

        try (InputStream fileInput
            = new BufferedInputStream(new FileInputStream(inputFile));
                OutputStream sigOut = new ArmoredOutputStream(
                    new FileOutputStream(signatureFile))) {
            int ch;
            while ((ch = fileInput.read()) >= 0) {
                signatureGenerator.update((byte) ch);
            }
            PGPSignature signature = signatureGenerator.generate();
            signature.encode(sigOut);
        }
    }
}
