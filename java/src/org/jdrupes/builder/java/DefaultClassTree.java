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

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Proxyable;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.DefaultFileTree;
import org.jdrupes.builder.core.ForwardingHandler;

/// A default implementation of [ClassTree].
///
public class DefaultClassTree extends DefaultFileTree<ClassFile>
        implements ClasspathElement {

    /// Initializes a new class tree.
    ///
    /// @param type the type
    /// @param project the project
    /// @param root the root
    ///
    protected DefaultClassTree(ResourceType<? extends ClassTree> type,
            Project project, Path root) {
        super(type, project, root, "**/*.class");
    }

    /// Creates an instance of [ClassTree].
    ///
    /// @param <T> the generic type
    /// @param type the type
    /// @param project the project
    /// @param path the path
    /// @return the class tree
    ///
    @SuppressWarnings({ "unchecked" })
    public static <T extends ClassTree> T createClassTree(ResourceType<T> type,
            Project project, Path path) {
        return (T) Proxy.newProxyInstance(type.rawType().getClassLoader(),
            new Class<?>[] { type.rawType(), Proxyable.class },
            new ForwardingHandler(new DefaultClassTree(type, project, path)));
    }

    @Override
    public Path toPath() {
        return root();
    }

}
