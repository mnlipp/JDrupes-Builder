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
import org.jdrupes.builder.java.JarFile;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.MvnRepoJarFileType;

/// A [JarFile] that has been downloaded from a Maven repository.
/// Unlike a [MvnRepoResource], a resource of this type represents a
/// local file. This interface merely preserves information about the
/// Maven repository resource from which it originated.
/// 
public interface MvnRepoJarFile extends JarFile {

    /// Returns the Maven repository reference for this JAR file.
    ///
    /// @return the Maven repository resource
    ///
    MvnRepoResource reference();

    /// Returns the repositories used to resolve this JAR file.
    ///
    /// @return the repositories
    ///
    List<RemoteRepository> repositories();

    /// Creates a new Maven repository JAR file resource from the given values.
    ///
    /// @param repositories the repositories
    /// @param coordinates the coordinates
    /// @param path the path
    /// @return the maven repository jar file
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    static MvnRepoJarFile of(List<RemoteRepository> repositories,
            String coordinates, Path path) {
        return ResourceFactory.create(
            MvnRepoJarFileType, repositories, coordinates, path);
    }
}
