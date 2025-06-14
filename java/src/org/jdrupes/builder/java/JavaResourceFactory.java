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

package org.jdrupes.builder.java;

import java.nio.file.Path;
import java.util.Optional;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.DefaultFileResource;
import static org.jdrupes.builder.java.JavaTypes.*;

/// A factory for creating Java related resource objects.
///
public class JavaResourceFactory implements ResourceFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Resource> Optional<T> newResource(ResourceType<T> type,
            Project project, Object... args) {
        if (JavaSourceFileType.equals(type) || ClassFileType.equals(type)
            || JavadocDirectoryType.equals(type)) {
            return Optional.of((T) DefaultFileResource.create(
                (ResourceType<? extends FileResource>) type, (Path) args[0]));
        }
        if (ClassTreeType.equals(type)) {
            return Optional.of((T) new ClassTree(null,
                (Path) args[0], (String) args[1]));
        }
        if (JarFileType.equals(type)) {
            return Optional.of((T) new JarFile((Path) args[0]));
        }
        if (AppJarFileType.equals(type)) {
            return Optional.of((T) new AppJarFile((Path) args[0]));
        }
        if (JavaResourceTree.class.equals(type.type())) {
            return Optional.of((T) new JavaResourceTree(project,
                (Path) args[0], (String) args[1], false));
        }
        return Optional.empty();
    }

}
