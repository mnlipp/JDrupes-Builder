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

import com.google.common.flogger.FluentLogger;
import static com.google.common.flogger.StackSize.*;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Renamable;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceProviderSpi;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;

/// A base implementation for[ResourceProvider]s.
///
public abstract class AbstractProvider implements ResourceProvider {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private String name;

    /// Initializes a new abstract provider.
    ///
    public AbstractProvider() {
        // Default name
        name = getClass().getSimpleName();
        if (name.isBlank()) {
            name = "Adapted " + getClass().getSuperclass().getSimpleName();
        }
    }

    @Override
    public String name() {
        return name;
    }

    /// Allows derived classes to change the provider's name in their
    /// implementation of [Renamable#name]. Throws an
    /// [UnsupportedOperationException] if the derived class does not
    /// implement [Renamable].
    ///
    /// @param name the name
    /// @return the abstract provider
    ///
    protected final AbstractProvider rename(String name) {
        if (!(this instanceof Renamable)) {
            throw new UnsupportedOperationException(getClass().getName()
                + " does not implement " + Renamable.class.getName());
        }
        this.name = name;
        return this;
    }

    /* default */ ResourceProviderSpi toSpi() {
        return new ResourceProviderSpi() {
            @Override
            public <T extends Resource> Stream<T>
                    provide(ResourceRequest<T> requested) {
                if (!FutureStream.isProviderInvocationAllowed()) {
                    logger.atWarning().withStackTrace(MEDIUM)
                        .log("Direct invocation of %s is not allowed", this);
                }
                return doProvide(requested);
            }
        };
    }

    /// Invoked by [ResourceProviderSpi#provide] after checking if the
    /// invocation is allowed.
    ///
    /// @param <T> the generic type
    /// @param requested the requested
    /// @return the stream
    ///
    protected abstract <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested);

    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public <T extends Resource> ResourceRequest<T>
            of(ResourceType<? extends T> type) {
        return new DefaultResourceRequest<>(type);
    }

    @Override
    public DefaultBuildContext context() {
        return LauncherSupport.buildContext();
    }

    /// Convenience function to create a new resource. Short for invoking
    /// [ResourceFactory#create] with the requested resource type,
    /// `null` for the project and the remaining given arguments appended.
    ///
    /// @param <T> the resource type
    /// @param type the type
    /// @param args the arguments
    /// @return the resource
    ///
    protected <T extends Resource> T newResource(ResourceType<T> type,
            Object... args) {
        return ResourceFactory.create(type, null, args);
    }

    /// Retrieves the resources as a Vavr stream.
    ///
    /// @return the stream
    ///
    public <T extends Resource> io.vavr.collection.Stream<T>
            vavrStream(Resources<T> resources) {
        return io.vavr.collection.Stream.ofAll(resources.stream());
    }

    @Override
    public String toString() {
        return "Provider " + getClass().getSimpleName();
    }
}
