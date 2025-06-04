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

package org.jdrupes.builder.core;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFile;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;

/// A provider for resources, usually from directories, to be included in a
/// (Java) project. 
///
public class ResourcesCollector
        extends AbstractGenerator<FileTree<ResourceFile>> {

    private final Resources<FileTree<ResourceFile>> fileTrees
        = project().newResources(Resource.class);

    /// Instantiates a new resources collector.
    ///
    /// @param project the project
    ///
    public ResourcesCollector(Project project) {
        super(project);
    }

    /// Adds the given file tree with resource directories.
    ///
    /// @param resources the resources
    /// @return the resources collector
    ///
    public final ResourcesCollector add(FileTree<ResourceFile> resources) {
        this.fileTrees.add(resources);
        return this;
    }

    /// Adds the given file trees with resource directories.
    ///
    /// @param resources the resources
    /// @return the resources collector
    ///
    public final ResourcesCollector
            add(Stream<FileTree<ResourceFile>> resources) {
        this.fileTrees.addAll(resources);
        return this;
    }

    /// Adds the files from the given directory matching the given pattern.
    /// Short for
    /// `add(project().newFileTree(directory, pattern, ResourceFile.class))`.
    ///
    /// @param directory the directory
    /// @param pattern the pattern
    /// @return the resources collector
    ///
    public final ResourcesCollector add(Path directory, String pattern) {
        add(project().newFileTree(directory, pattern, ResourceFile.class));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Resource> Stream<R>
            provide(ResourceRequest<R> requested) {
        if (!requested.type().isAssignableFrom(ResourceType.RESOURCE_FILES)) {
            return Stream.empty();
        }
        return (Stream<R>) fileTrees.stream();
    }

}
