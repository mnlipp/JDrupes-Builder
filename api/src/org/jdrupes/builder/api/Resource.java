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

package org.jdrupes.builder.api;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

/// Represents a resource handled by the builder. Resources can have names
/// that may be referenced in [ResourceRequest]s.
///
public interface Resource {

    /// The instant at which this resource was created or last modified.
    ///
    /// @return the instant
    ///
    default Optional<Instant> asOf() {
        return Optional.empty();
    }

    /// Checks if this resource is newer than the other.
    ///
    /// @param other the other
    /// @return true, if is newer than
    ///
    @SuppressWarnings("PMD.SimplifyBooleanReturns")
    default boolean isNewerThan(Resource other) {
        if (asOf().isEmpty() && other.asOf().isEmpty()) {
            return false;
        }
        if (asOf().isEmpty()) {
            return false;
        }
        if (other.asOf().isEmpty()) {
            return true;
        }
        return asOf().get().isAfter(other.asOf().get());
    }

    /// Returns the type of this resource.
    ///
    /// @return the type
    ///
    ResourceType<?> type();

    /// Returns the name of this resource if set.
    ///
    /// @return the optional
    ///
    Optional<String> name();

    /// Cleanup the resource. This method is called by [ResourceProvider]s
    /// in response to a request for [Cleanliness]. The default
    /// implementation does nothing. Derived classes should override this
    /// and delete any physical representation of the resource.
    ///
    default void cleanup() {
        // Do nothing
    }

    /// Returns a localized string representation of the instant
    /// at which this resource was created or last modified.
    ///
    /// @return the string
    ///
    default String asOfLocalized() {
        var asOf = asOf();
        if (asOf.isEmpty()) {
            return "non-existant";
        }
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .format(asOf.get().atZone(ZoneId.systemDefault()));
    }
}
