/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
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

package org.jdrupes.builder.ext.pmd;

import java.lang.reflect.Proxy;
import java.util.Optional;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Proxyable;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.ForwardingHandler;

/// A factory for creating the PMD extension resource objects.
///
public class PmdResourceFactory implements ResourceFactory {

    /// Instantiates a new PMD resource factory.
    ///
    public PmdResourceFactory() {
        // Make javadoc happy.
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Resource> Optional<T> newResource(ResourceType<T> type,
            Project project, Object... args) {
        if (PmdTypes.PmdResultType.isAssignableFrom(type)) {
            return Optional.of((T) Proxy.newProxyInstance(
                type.rawType().getClassLoader(),
                new Class<?>[] { type.rawType(), Proxyable.class },
                new ForwardingHandler(new DefaultPmdResult(project,
                    (ResourceProvider) args[0], (long) args[1],
                    (long) args[2]))));
        }
        return Optional.empty();
    }
}
