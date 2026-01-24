/*
 * JDrupes Builder
 * Copyright (C) 2025, 2026 Michael N. Lipp
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

package org.jdrupes.builder.api;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/// Represents a request for [Resource]s of a specified type, to be
/// processed by a [ResourceProvider].
/// 
/// When requesting resources from a [Project], the [Project] forwards
/// the request to its dependencies that have been associated with one
/// of the [Intent]s specified with [#using(Set)].
///
/// @param <T> the requested type
///
public interface ResourceRequest<T extends Resource> extends Cloneable {

    /// Return the requested type.
    ///
    /// @return the resource type
    ///
    ResourceType<? extends T> type();

    /// Return a new resource request for a resource with the given name.
    /// 
    /// Support for resource names is optional and provider-specific.
    /// Expect the name to be ignored if not explicitly supported for
    /// a given resource type and provider.
    ///
    /// @param name the name
    /// @return the resource request
    ///
    ResourceRequest<T> withName(String name);

    /// Returns the name that the requested resource should have.
    ///
    /// @return the name if set
    ///
    Optional<String> name();

    /// Return a new resource request that uses project providers with
    /// the given intents.
    ///
    /// @param intents the intents
    /// @return the resource request
    ///
    ResourceRequest<T> using(Set<Intent> intents);

    /// Return a new resource request that uses a project's providers
    /// with the given intents.
    ///
    /// @param intent the intent
    /// @param intents the intents
    /// @return the resource request
    ///
    default ResourceRequest<T> using(Intent intent, Intent... intents) {
        return using(EnumSet.of(intent, intents));
    }

    /// Return a new resource request that uses all providers of projects.
    ///
    /// @return the resource request
    ///
    default ResourceRequest<T> usingAll() {
        return using(EnumSet.allOf(Intent.class));
    }

    /// Returns the intents to be used for selecting providers.
    ///
    /// @return the sets the
    ///
    Set<Intent> uses();

    /// Checks if the query accepts results of the given type. This
    /// is short for `type().isAssignableFrom(type)`. 
    ///
    /// @param type the type to check
    /// @return true, if successful
    ///
    boolean accepts(ResourceType<?> type);

    /// Checks if the query requires results of the given type. This
    /// is short for `type.isAssignableFrom(type())`. 
    ///
    /// @param type the type to check
    /// @return true, if successful
    ///
    boolean requires(ResourceType<?> type);
}
