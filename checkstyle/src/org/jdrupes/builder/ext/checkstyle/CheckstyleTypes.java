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

package org.jdrupes.builder.ext.checkstyle;

import org.jdrupes.builder.api.ResourceType;

/// Resource types for the checkstyle extension.
///
public final class CheckstyleTypes {

    private CheckstyleTypes() {
    }

    /// Type for [CheckstyleResult].
    ///
    @SuppressWarnings("PMD.FieldNamingConventions")
    public static final ResourceType<CheckstyleResult> CheckstyleResultType
        = new ResourceType<>() {};
}
