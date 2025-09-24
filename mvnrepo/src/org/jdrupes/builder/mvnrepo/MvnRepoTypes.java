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

import org.jdrupes.builder.api.ResourceType;

/// A collection of Maven specific [ResourceType]s.
///
@SuppressWarnings({ "PMD.FieldNamingConventions", "PMD.DataClass" })
public final class MvnRepoTypes {

    private MvnRepoTypes() {
    }

    /// The maven repository resource type.
    public static final ResourceType<MvnRepoResource> MvnRepoResourceType
        = new ResourceType<>() {};

    /// The maven repository dependency type.
    public static final ResourceType<
            MvnRepoDependency> MvnRepoDependencyType = new ResourceType<>() {};

    /// The maven repository dependencies type.
    public static final ResourceType<
            MvnRepoDependencies> MvnRepoDependenciesType
                = new ResourceType<>() {};

    /// The maven publication type.
    public static final ResourceType<MvnPublication> MvnPublicationType
        = new ResourceType<>() {};

    /// The POM file type.
    public static final ResourceType<PomFile> PomFileType
        = new ResourceType<>() {};

}
