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

package org.jdrupes.builder.core;

import java.util.Collection;
import org.jdrupes.builder.api.Renamable;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;

/// Provided resources are identified by the [ResourceProvider]
/// and the requested [Resource].
///
/// @param <T> the generic type
/// @param provider the provider
/// @param request the requested resources
///
public record ProviderInvocation<T extends Resource>(ResourceProvider provider,
        ResourceRequest<T> request) {

    private static class Launcher extends AbstractProvider
            implements Renamable {
        private Launcher() {
            name("Launcher");
        }

        @Override
        public Renamable name(String name) {
            super.rename(name);
            return this;
        }

        protected <T extends Resource> Collection<T>
                doProvide(ResourceRequest<T> request) {
            throw new UnsupportedOperationException();
        }
    }

    /// The launching invocation.
    public static final ProviderInvocation<?> LAUNCH
        = new ProviderInvocation<>(new Launcher(),
            new DefaultResourceRequest<>(new ResourceType<Resource>() {}));

    @Override
    public String toString() {
        if (provider.equals(LAUNCH.provider)) {
            return "Launcher";
        }
        return "[" + provider + " ← "
            + ((DefaultResourceRequest<?>) request).toRequestedString() + "]";
    }

}