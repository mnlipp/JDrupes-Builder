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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.layout.RepositoryLayout;

/// A [RepositoryLayout] decorator that suppresses checksum file uploads for
/// metadata.
///
/// Eclipse Aether attempts to upload `.sha1` and `.md5` checksum files for
/// `maven-metadata.xml` during a deployment. Some remote repositories, notably
/// forgejo maven repository implementations, reject these
/// checksum uploads with HTTP 400. This layout wraps a delegate layout and
/// returns an empty list from [getChecksumLocations] for metadata when
/// uploading, so that no checksum files are generated for metadata. Checksum
/// uploads for regular artifacts (jar, pom, etc.) are unaffected.
///
final class NoMetadataChecksumLayout implements RepositoryLayout {

    private final RepositoryLayout delegate;

    /// Wraps the given [RepositoryLayout].
    ///
    /// @param delegate the delegate layout to wrap
    ///
    /* default */ NoMetadataChecksumLayout(RepositoryLayout delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public List<ChecksumAlgorithmFactory>
            getChecksumAlgorithmFactories() {
        return delegate.getChecksumAlgorithmFactories();
    }

    @Override
    public List<ChecksumAlgorithmFactory>
            getChecksumAlgorithmFactories(boolean upload) {
        return delegate.getChecksumAlgorithmFactories(upload);
    }

    @Override
    public boolean hasChecksums(Artifact artifact) {
        return delegate.hasChecksums(artifact);
    }

    @Override
    public URI getLocation(
            Artifact artifact, boolean upload) {
        return delegate.getLocation(artifact, upload);
    }

    @Override
    public URI getLocation(
            Metadata metadata, boolean upload) {
        return delegate.getLocation(metadata, upload);
    }

    @Override
    public List<ChecksumLocation> getChecksumLocations(
            Artifact artifact,
            boolean upload,
            URI location) {
        return delegate.getChecksumLocations(
            artifact, upload, location);
    }

    @Override
    public List<ChecksumLocation> getChecksumLocations(
            Metadata metadata,
            boolean upload,
            URI location) {

        if (upload) {
            return Collections.emptyList();
        }

        return delegate.getChecksumLocations(
            metadata, false, location);
    }
}