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

package org.jdrupes.builder.mvnrepo;

import org.jdrupes.builder.api.PropertyKey;

/// Additional properties used with maven repositories.
///
public final class MvnProperties {

    /// The group that the project's artifacts belong to. Defaults to `null`.
    @SuppressWarnings("PMD.FieldNamingConventions")
    public static final PropertyKey<String> GroupId
        = new PropertyKey<>(String.class);

    /// The artifact id. Defaults to `null`.
    @SuppressWarnings("PMD.FieldNamingConventions")
    public static final PropertyKey<String> ArtifactId
        = new PropertyKey<>(String.class);

    /// A key that can be used for centralized management of
    /// [MvnPublishingDestination]s. The pattern is to set the
    /// value of this property in the root project and then use
    /// `generator(MvnPublisher::new).destination(get(PublishingDestinations));`
    /// in each project when adding a [MvnPublisher].
    @SuppressWarnings("PMD.FieldNamingConventions")
    public static final PropertyKey<
            MvnPublishingDestination[]> PublishingDestinations
                = new PropertyKey<>(MvnPublishingDestination[].class);

    private MvnProperties() {
    }
}