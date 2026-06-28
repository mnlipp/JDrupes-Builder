/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
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
import java.io.BufferedOutputStream;
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
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.bouncycastle.util.encoders.Base64;
import org.eclipse.aether.artifact.Artifact;
import org.jdrupes.builder.api.BuildContext;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ConfigurationException;
import static org.jdrupes.builder.mvnrepo.MvnProperties.ArtifactId;

/// A Maven publishing destination that publishes releases using the
/// [Sonatype Publish Portal API](https://central.sonatype.org/publish/publish-portal-api/).
///
/// Instead of uploading files individually, this implementation of
/// [MvnPublishingDestination] bundles all artifacts into a single ZIP
/// release bundle and uploads it via a multipart HTTP request. It is the
/// modern recommended way to publish releases to Maven Central.
///
public class PortalPublisherDestination extends MvnPublishingDestination {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private boolean publishAutomatically;
    private URI uploadUri = URI
        .create("https://central.sonatype.com/api/v1/publisher/upload");

    /// Initializes a new portal publisher destination.
    /// 
    /// The id is initialized with "central". This allows the credentials
    /// from the server section in `settings.xml` with this id to be used
    /// as fallbacks.
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public PortalPublisherDestination() {
        super(PublicationType.RELEASE);
        id("central");
    }

    /// Publish the release automatically.
    ///
    /// @return this destination
    ///
    public PortalPublisherDestination publishAutomatically() {
        publishAutomatically = true;
        return this;
    }

    /// Sets the upload URI.
    ///
    /// @param uri the repository URI
    /// @return this destination
    ///
    public PortalPublisherDestination uploadUri(URI uri) {
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

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    /* default */void publish(BuildContext context, MvnPublisher publisher,
            Artifact mainArtifact, List<Artifact> toDeploy) {
        var project = publisher.project();
        // Create zip file with all artifacts for release, see
        // https://central.sonatype.org/publish/publish-portal-upload/
        var zipName = Optional.ofNullable(project.get(ArtifactId))
            .orElse(project.name()) + "-" + mainArtifact.getVersion()
            + "-release.zip";
        var zipPath = publisher.artifactDirectory().resolve(zipName);
        try {
            Path praefix = Path.of(mainArtifact.getGroupId().replace('.', '/'))
                .resolve(mainArtifact.getArtifactId())
                .resolve(mainArtifact.getVersion());
            try (ZipOutputStream zos
                = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                for (var artifact : toDeploy) {
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
                        artifact.getPath())) {
                        fis.transferTo(zos);
                    }
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new BuildException().from(publisher).cause(e);
        }

        try (var client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMinutes(1)).build()) {
            var boundary = "===" + System.currentTimeMillis() + "===";
            var user = repositoryUser(context);
            var password = repositoryPassword(context);
            var token = new String(Base64.encode((user + ":" + password)
                .getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            var effectiveUri = uploadUri;
            if (publishAutomatically) {
                effectiveUri = addQueryParameter(
                    uploadUri, "publishingType", "AUTOMATIC");
            }
            HttpRequest request = HttpRequest.newBuilder().uri(effectiveUri)
                .timeout(Duration.ofMinutes(10))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type",
                    "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers
                    .ofInputStream(() -> getAsMultipart(zipPath, boundary)))
                .build();
            logger.atInfo().log("Uploading release bundle...");
            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
            logger.atFinest().log("Upload response: %s", response.body());
            if (response.statusCode() / 100 != 2) {
                throw new ConfigurationException().from(publisher).message(
                    "Failed to upload release bundle: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new BuildException().from(publisher).cause(e);
        }
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

}
