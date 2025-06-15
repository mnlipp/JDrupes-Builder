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
import java.util.Arrays;
import java.util.Optional;
import static java.util.function.Predicate.not;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;
import static org.jdrupes.builder.core.CoreTypes.*;

/// A factory for creating the Core resource objects.
///
public class CoreResourceFactory implements ResourceFactory {

    /// Gets all interfaces that the given class implements,
    /// including the class itself.
    ///
    /// @param clazz the clazz
    /// @return all interfaces
    ///
    public static Stream<Class<?>> getAllInterfaces(Class<?> clazz) {
        return Stream.concat(Stream.of(clazz),
            Arrays.stream(clazz.getInterfaces())
                .map(CoreResourceFactory::getAllInterfaces).flatMap(s -> s));
    }

    /// Checks if the derived interface adds any methods to the
    /// base interface.
    ///
    /// @param <T> the generic type
    /// @param base the base
    /// @param derived the derived
    /// @return true, if successful
    ///
    public static <T> boolean addsMethod(
            Class<T> base, Class<? extends T> derived) {
        var baseItfs = getAllInterfaces(base).collect(Collectors.toSet());
        return getAllInterfaces(derived).filter(not(baseItfs::contains))
            .filter(itf -> itf.getDeclaredMethods().length > 0).findAny()
            .isPresent();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Resource> Optional<T> newResource(ResourceType<T> type,
            Project project, Object... args) {
        if (FileResourceType.isAssignableFrom(type)
            && type.type().getSuperclass() == null
            && !addsMethod(FileResource.class,
                (Class<? extends FileResource>) type.type())) {
            return Optional.of((T) DefaultFileResource.createFileResource(
                (ResourceType<? extends FileResource>) type, (Path) args[0]));
        }
        if (Resources.class.equals(type.type())
            && type.type().getSuperclass() == null
            && !addsMethod(Resources.class,
                (Class<? extends Resources<?>>) type.type())) {
            return Optional.of((T) DefaultResources.createResources(
                (ResourceType<? extends Resources<?>>) type));
        }
        if (FileTree.class.equals(type.type())
            && type.type().getSuperclass() == null
            && !addsMethod(FileTree.class,
                (Class<? extends FileTree<?>>) type.type())) {
            return Optional.of(
                (T) DefaultFileTree.createFileTree((ResourceType<? extends FileTree<?>>) type,
                    project,
                    (Path) args[0], (String) args[1],
                    args.length > 2 && (boolean) args[2]));
        }
        return Optional.empty();
    }

}
