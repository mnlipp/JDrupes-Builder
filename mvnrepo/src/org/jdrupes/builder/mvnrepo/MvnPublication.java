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

import org.jdrupes.builder.api.ResourceFactory;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.MvnPublicationType;

/// Represents an artifact created in a maven repository.
///
public interface MvnPublication extends MvnRepoResource {

    /// Creates a maven publication from Maven coordinates.
    ///
    /// @param coordinates the coordinates
    /// @return the maven publication
    ///
    static MvnPublication from(String coordinates) {
        return ResourceFactory.create(MvnPublicationType, coordinates);
    }
}
