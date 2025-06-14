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
import java.util.Optional;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;
import static org.jdrupes.builder.core.CoreTypes.ResourceFileType;

/// A factory for creating the Core resource objects.
///
public class CoreResourceFactory implements ResourceFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Resource> Optional<T> newResource(ResourceType<T> type,
            Project project, Object... args) {
        if (ResourceFileType.equals(type)) {
            return Optional.of((T) DefaultFileResource.create(
                (ResourceType<? extends FileResource>) type, (Path) args[0]));
        }
        if (Resources.class.equals(type.type())) {
            return Optional.of((T) DefaultResources.create(
                (ResourceType<? extends Resources<?>>) type));
        }
        if (FileTree.class.equals(type.type())) {
            return Optional.of(
                (T) DefaultFileTree.create(project,
                    (ResourceType<? extends FileTree<?>>) type,
                    (Path) args[0], (String) args[1],
                    args.length > 2 && (boolean) args[2]));
        }
        return Optional.empty();
    }

}
