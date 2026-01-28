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

package org.jdrupes.builder.bnd;

import java.util.Collections;
import java.util.Map;
import org.jdrupes.builder.api.PropertyKey;

/// Bnd specific property keys.
///
public enum BndProperties implements PropertyKey {

    /// Supports the definition of Bnd instructions as project properties.
    /// Typically, this property is set for the root project. Note that
    /// the [BndAnalyzer] does not pick up these instructions automatically.
    /// Rather, they have to be added with [BndAnalyzer#instructions(Map)]
    /// explicitly.
    ///  
    @SuppressWarnings("PMD.FieldNamingConventions")
    BndInstructions;
    
    @Override
    public Class<?> propertyType() {
        return Map.class;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T defaultValue() {
        return (T) Collections.emptyMap();
    }
}