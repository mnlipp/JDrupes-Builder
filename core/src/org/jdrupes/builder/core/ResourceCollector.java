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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;

/// The Class ResourceCollector.
///
public class ResourceCollector extends AbstractGenerator<FileTree> {

    private final List<FileTree> fileSets = new ArrayList<>();

    public ResourceCollector(Project project) {
        super(project);
    }

    public ResourceCollector add(FileTree fileSet) {
        fileSets.add(fileSet);
        return this;
    }

    @Override
    public Stream<FileTree> provide(Resource requested) {
        if (!Resource.KIND_RESOURCES.equals(requested.kind())) {
            return Stream.empty();
        }
        return fileSets.stream();
    }

}
