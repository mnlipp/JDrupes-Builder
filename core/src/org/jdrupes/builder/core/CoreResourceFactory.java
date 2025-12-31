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

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import static java.util.function.Predicate.not;
import java.util.stream.Collectors;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.api.TestResult;

/// A factory for creating the Core resource objects.
///
public class CoreResourceFactory implements ResourceFactory {

    /// Instantiates a new core resource factory.
    ///
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public CoreResourceFactory() {
        // Make javadoc happy.
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
        var baseItfs = ResourceType.getAllInterfaces(base)
            .collect(Collectors.toSet());
        return ResourceType.getAllInterfaces(derived)
            .filter(not(baseItfs::contains))
            .filter(itf -> Arrays.stream(itf.getDeclaredMethods())
                .filter(not(Method::isDefault)).findAny().isPresent())
            .findAny().isPresent();
    }

    @Override
    @SuppressWarnings({ "unchecked", "PMD.CyclomaticComplexity",
        "PMD.NPathComplexity" })
    public <T extends Resource> Optional<T> newResource(ResourceType<T> type,
            Project project, Object... args) {
        if (ResourceType.FileResourceType.isAssignableFrom(type)
            && type.rawType().getSuperclass() == null
            && !addsMethod(FileResource.class,
                (Class<? extends FileResource>) type.rawType())) {
            return Optional.of((T) DefaultFileResource.createFileResource(
                (ResourceType<? extends FileResource>) type, (Path) args[0]));
        }
        if (ResourceType.TestResultType.isAssignableFrom(type)
            && type.rawType().getSuperclass() == null
            && !addsMethod(TestResult.class,
                (Class<? extends TestResult>) type.rawType())) {
            return Optional.of((T) DefaultTestResult.createTestResult(
                (ResourceType<? extends TestResult>) type, (String) args[0],
                (int) args[1], (int) args[2]));
        }
        if (Resources.class.isAssignableFrom(type.rawType())
            && type.rawType().getSuperclass() == null
            && !addsMethod(Resources.class,
                (Class<? extends Resources<?>>) type.rawType())) {
            return Optional.of((T) DefaultResources.createResources(
                (ResourceType<? extends Resources<?>>) type));
        }
        if (FileTree.class.isAssignableFrom(type.rawType())
            && type.rawType().getSuperclass() == null
            && !addsMethod(FileTree.class,
                (Class<? extends FileTree<?>>) type.rawType())) {
            return Optional.of(
                (T) DefaultFileTree.createFileTree(
                    (ResourceType<? extends FileTree<?>>) type,
                    project, (Path) args[0], (String) args[1]));
        }
        if (Resource.class.isAssignableFrom(type.rawType())
            && type.rawType().getSuperclass() == null
            && !addsMethod(Resource.class,
                (Class<? extends Resource>) type.rawType())) {
            return Optional.of((T) ResourceObject.createResource(
                (ResourceType<?>) type));
        }
        return Optional.empty();
    }

}
