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

import java.util.Objects;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.layout.RepositoryLayout;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutFactory;
import org.eclipse.aether.transfer.NoRepositoryLayoutException;

/// A [RepositoryLayoutFactory] decorator that produces instances of
/// [NoMetadataChecksumLayout]. See [NoMetadataChecksumLayout] for details.
///
final class NoMetadataChecksumLayoutFactory
        implements RepositoryLayoutFactory {

    private final RepositoryLayoutFactory delegate;

    /// Wraps the given [RepositoryLayoutFactory].
    ///
    /// @param delegate the delegate factory to wrap
    ///
    /* default */ NoMetadataChecksumLayoutFactory(
            RepositoryLayoutFactory delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public float getPriority() {
        return delegate.getPriority();
    }

    @Override
    public RepositoryLayout newInstance(
            RepositorySystemSession session,
            RemoteRepository repository)
            throws NoRepositoryLayoutException {

        return new NoMetadataChecksumLayout(
            delegate.newInstance(session, repository));
    }
}