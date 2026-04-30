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

package org.jdrupes.builder.api;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

/// Represents a resource that has readable contents.
///
public interface InputResource extends Resource {

    /// Returns the input stream.
    ///
    /// @return the input stream
    /// @throws IOException Signals that an I/O exception has occurred.
    ///
    InputStream inputStream() throws IOException;

    /// Creates a new input resource.
    ///
    /// @param asOf the modification date
    /// @param data the data
    /// @return the file resource
    ///
    @SuppressWarnings({ "PMD.UseDiamondOperator", "PMD.ShortMethodName" })
    static InputResource of(Instant asOf, InputStream data) {
        return ResourceFactory.create(new ResourceType<InputResource>() {},
            asOf, data);
    }
}
