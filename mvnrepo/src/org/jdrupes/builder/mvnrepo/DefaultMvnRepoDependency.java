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

import java.util.Objects;
import org.jdrupes.builder.api.ResourceType;

/// Represents an artifact in a maven repository.
///
public class DefaultMvnRepoDependency extends DefaultMvnRepoResource
        implements MvnRepoDependency {

    private final Scope scope;

    /// Instantiates a new default mvn repo dependency.
    ///
    /// @param type the type
    /// @param coordinate the coordinate
    /// @param scope the scope
    ///
    public DefaultMvnRepoDependency(
            ResourceType<? extends MvnRepoDependency> type,
            String coordinate, Scope scope) {
        super(type, coordinate);
        this.scope = Objects.requireNonNull(scope);
    }

    @Override
    public Scope scope() {
        return scope;
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        return type().toString() + " " + coordinates() + " (" + scope + ")";
    }
}
