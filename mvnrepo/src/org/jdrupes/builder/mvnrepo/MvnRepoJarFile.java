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
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.java.JarFile;

/// A [JarFile] that is obtained from a maven repository.
///
public interface MvnRepoJarFile extends JarFile {

    /// The Maven repository resource reference for this jar file.
    ///
    /// @return the mvn repo resource
    ///
    MvnRepoResource reference();

    /// Creates a new maven repository jar file resource from the given values.
    ///
    /// @param <T> the resource type
    /// @param fileType the requested type
    /// @param coordinates the coordinates
    /// @param path the path
    /// @return the maven repository jar file
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    static <T extends JarFile> T of(ResourceType<T> fileType,
            String coordinates, Path path) {
        return ResourceFactory.create(fileType, coordinates, path);
    }
}
