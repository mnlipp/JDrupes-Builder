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

import java.nio.file.Path;
import java.util.List;
import org.eclipse.aether.repository.RemoteRepository;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.java.LibraryJarFile;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.MvnRepoLibraryJarFileType;

/// A [LibraryJarFile] that is obtained from a maven repository.
///
public interface MvnRepoLibraryJarFile extends MvnRepoJarFile, LibraryJarFile {

    /// Creates a new Maven repository library JAR file resource
    /// from the given values.
    ///
    /// @param repositories the repositories
    /// @param coordinates the coordinates
    /// @param path the path
    /// @return the maven repository jar file
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    static MvnRepoLibraryJarFile of(List<RemoteRepository> repositories,
            String coordinates, Path path) {
        return ResourceFactory.create(MvnRepoLibraryJarFileType,
            repositories, coordinates, path);
    }

}
