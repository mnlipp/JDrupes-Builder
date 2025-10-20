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

package org.jdrupes.builder.eclipse;

import java.util.Objects;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.ResourceObject;

/// Represents an eclipse configuration.
///
public class DefaultEclipseConfiguration extends ResourceObject
        implements EclipseConfiguration {

    private final String projectName;
    private final String eclipseAlias;

    /// Initializes a new default eclipse configuration.
    ///
    /// @param type the type
    /// @param projectName the project name
    /// @param eclipseAlias the eclipse alias
    ///
    public DefaultEclipseConfiguration(
            ResourceType<? extends EclipseConfiguration> type,
            String projectName, String eclipseAlias) {
        super(type);
        this.projectName = Objects.requireNonNull(projectName);
        this.eclipseAlias = Objects.requireNonNull(eclipseAlias);
    }

    @Override
    public String projectName() {
        return projectName;
    }

    @Override
    public String eclipseAlias() {
        return eclipseAlias;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(projectName);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof DefaultEclipseConfiguration)) {
            return false;
        }
        DefaultEclipseConfiguration other = (DefaultEclipseConfiguration) obj;
        return Objects.equals(projectName, other.projectName);
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        return type().toString() + " " + eclipseAlias
            + " (" + projectName + ")";
    }
}
