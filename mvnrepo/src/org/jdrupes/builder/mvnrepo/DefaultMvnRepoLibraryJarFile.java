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

import java.nio.file.Path;
import java.util.List;
import org.eclipse.aether.repository.RemoteRepository;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.java.DefaultJarFile;
import org.jdrupes.builder.java.LibraryJarFile;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.MvnRepoResourceType;

/// The default implementation of a [MvnRepoLibraryJarFile].
///
public class DefaultMvnRepoLibraryJarFile extends DefaultJarFile
        implements MvnRepoLibraryJarFile {

    private final List<RemoteRepository> repositories;
    private final MvnRepoResource resource;

    /// Initializes a new instance with the given values.
    ///
    /// @param type the type
    /// @param repositories the repositories
    /// @param coordinates the coordinates
    /// @param path the path
    ///
    public DefaultMvnRepoLibraryJarFile(
            ResourceType<? extends LibraryJarFile> type,
            List<RemoteRepository> repositories, String coordinates,
            Path path) {
        super(type, path);
        this.repositories = repositories;
        resource = new DefaultMvnRepoResource(MvnRepoResourceType, coordinates);
    }

    @Override
    public List<RemoteRepository> repositories() {
        return repositories;
    }

    @Override
    public MvnRepoResource reference() {
        return resource;
    }
}
