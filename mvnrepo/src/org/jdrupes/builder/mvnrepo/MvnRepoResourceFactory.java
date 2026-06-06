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
import java.util.Optional;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.core.CoreResourceFactory.createNarrowed;

/// A factory for creating Maven repository related resource objects.
///
public class MvnRepoResourceFactory implements ResourceFactory {

    /// Instantiates a new Maven repository resource factory.
    ///
    public MvnRepoResourceFactory() {
        // Make javadoc happy
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Resource> Optional<T> newResource(ResourceType<T> type,
            Project project, Object... args) {
        // ? extends MvnRepoResource
        var candidate = createNarrowed(type, MvnRepoResource.class,
            () -> new DefaultMvnRepoResource(
                (ResourceType<? extends MvnRepoResource>) type,
                (String) args[0]));
        if (candidate.isPresent()) {
            return candidate;
        }

        // ? extends MvnRepoLibraryJar
        candidate = createNarrowed(type, MvnRepoLibraryJarFile.class,
            () -> new DefaultMvnRepoLibraryJarFile(
                (ResourceType<? extends MvnRepoLibraryJarFile>) type,
                (String) args[0], (Path) args[1]));
        if (candidate.isPresent()) {
            return candidate;
        }

        // ? extends MvnRepoJar
        candidate = createNarrowed(type, MvnRepoJarFile.class,
            () -> new DefaultMvnRepoJarFile(
                (ResourceType<? extends MvnRepoJarFile>) type,
                (String) args[0], (Path) args[1]));
        return candidate;
    }

}
