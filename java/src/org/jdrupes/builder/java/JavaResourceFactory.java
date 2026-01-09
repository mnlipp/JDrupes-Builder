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
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.core.CoreResourceFactory.*;
import static org.jdrupes.builder.java.JavaTypes.*;

/// A factory for creating Java related resource objects.
///
public class JavaResourceFactory implements ResourceFactory {

    /// Instantiates a new java resource factory.
    ///
    public JavaResourceFactory() {
        // Make javadoc happy
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Resource> Optional<T> newResource(ResourceType<T> type,
            Project project, Object... args) {
        if (ClassTreeType.isAssignableFrom(type)
            && type.rawType().getSuperclass() == null
            && !addsMethod(ClassTree.class,
                (Class<? extends ClassTree>) type.rawType())) {
            return Optional
                .of((T) DefaultClassTree.createClassTree(
                    (ResourceType<? extends ClassTree>) type, project,
                    (Path) args[0]));
        }
        if (LibraryJarFileType.isAssignableFrom(type)
            && type.rawType().getSuperclass() == null
            && !addsMethod(LibraryJarFile.class,
                (Class<? extends LibraryJarFile>) type.rawType())) {
            return Optional.of((T) DefaultLibraryJarFile.createLibraryJarFile(
                (ResourceType<? extends JarFile>) type, (Path) args[0]));
        }
        if (JarFileType.isAssignableFrom(type)
            && type.rawType().getSuperclass() == null
            && !addsMethod(JarFile.class,
                (Class<? extends JarFile>) type.rawType())) {
            return Optional.of((T) DefaultJarFile.createJarFile(
                (ResourceType<? extends JarFile>) type, (Path) args[0]));
        }
        if (JavaResourceTree.class.equals(type.rawType())) {
            return Optional.of((T) new JavaResourceTree(project,
                (Path) args[0], (String) args[1]));
        }
        return Optional.empty();
    }

}
