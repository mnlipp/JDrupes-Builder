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
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import static java.util.function.Predicate.not;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jdrupes.builder.api.ExecResult;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Proxyable;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.api.TestResult;

/// A factory for creating the Core resource objects.
///
public class CoreResourceFactory implements ResourceFactory {

    /// Instantiates a new core resource factory.
    ///
    public CoreResourceFactory() {
        // Make javadoc happy.
    }

    /// Creates a narrowed resource. Given a wanted interface type, an
    /// implemented interface type and a supplier that returns an
    /// instance of the implemented type, returns an instance of the
    /// wanted type if possible.
    /// 
    /// Returning an implementation of the wanted type is possible if
    /// the following conditions are met:
    /// 
    ///  1. The wanted type has no superclass (i.e. is an interface).
    /// 
    ///  2. The wanted type is a subclass of the implemented type.
    /// 
    ///  3. The wanted type does not add any methods to the
    ///     implemented type.
    /// 
    /// The implementation uses a dynamic proxy to wrap the
    /// implemented instance together with a [ForwardingHandler],
    /// that simply forwards all invocations to the proxied object
    /// (hence the requirement that the wanted type does not add
    /// any methods to the implemented type).
    ///
    /// @param <T> the wanted type
    /// @param <I> the implemented (available) type
    /// @param wanted the wanted
    /// @param implemented the implemented interface
    /// @param supplier the supplier of a class that implements I
    /// @return an instance if possible
    ///
    @SuppressWarnings("unchecked")
    public static <T extends Resource, I extends Resource> Optional<T>
            createNarrowed(ResourceType<T> wanted, Class<I> implemented,
                    Supplier<? extends I> supplier) {
        if (implemented.isAssignableFrom(wanted.rawType())
            // we now know that T extends I
            && wanted.rawType().getSuperclass() == null
            && !addsMethod(implemented,
                (Class<? extends I>) wanted.rawType())) {
            return Optional.of(narrow(wanted, supplier.get()));
        }
        return Optional.empty();
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

    @SuppressWarnings({ "unchecked" })
    private static <T extends Resource> T narrow(ResourceType<T> type,
            Resource instance) {
        return (T) Proxy.newProxyInstance(type.rawType().getClassLoader(),
            new Class<?>[] { type.rawType(), Proxyable.class },
            new ForwardingHandler(instance));
    }

    /// New resource.
    ///
    /// @param <T> the generic type
    /// @param type the type
    /// @param project the project
    /// @param args the args
    /// @return the optional
    ///
    @Override
    @SuppressWarnings({ "unchecked" })
    public <T extends Resource> Optional<T> newResource(ResourceType<T> type,
            Project project, Object... args) {
        // ? extends FileResource
        var candidate = createNarrowed(type, FileResource.class,
            () -> new DefaultFileResource(
                (ResourceType<? extends FileResource>) type, (Path) args[0]));
        if (candidate.isPresent()) {
            return candidate;
        }

        // ? extends TestResult
        candidate = createNarrowed(type, TestResult.class,
            () -> new DefaultTestResult(project, (ResourceProvider) args[0],
                (String) args[1], (long) args[2], (long) args[3]));
        if (candidate.isPresent()) {
            return candidate;
        }

        // ? extends ExecResult
        candidate = createNarrowed(type, ExecResult.class,
            () -> new DefaultExecResult((ResourceProvider) args[0],
                (String) args[1], (int) args[2]));
        if (candidate.isPresent()) {
            return candidate;
        }

        // ? extends Resources
        candidate = createNarrowed(type, Resources.class,
            () -> new DefaultResources<>(
                (ResourceType<? extends Resources<?>>) type));
        if (candidate.isPresent()) {
            return candidate;
        }

        // ? extends FileTree
        candidate = createNarrowed(type, FileTree.class,
            () -> new DefaultFileTree<>(
                (ResourceType<? extends FileTree<?>>) type,
                project, (Path) args[0], (String) args[1]));
        if (candidate.isPresent()) {
            return candidate;
        }

        // Finally, try resource
        return createNarrowed(type, Resource.class,
            () -> new ResourceObject((ResourceType<?>) type) {});
    }

}
