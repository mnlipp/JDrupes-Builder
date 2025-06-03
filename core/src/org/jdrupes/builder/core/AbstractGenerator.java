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

package org.jdrupes.builder.core;

import java.util.logging.Logger;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;

/// A base implementation of a [Generator].
///
/// @param <T> the type of resource in the [Resources] container that
/// this generator provides
///
public abstract class AbstractGenerator<T extends Resource>
        implements Generator<T> {

    /// The log.
    protected final Logger log = Logger.getLogger(getClass().getName());

    private final Project project;
    private String name;

    /// Instantiates a new abstract generator.
    ///
    /// @param project the project
    ///
    public AbstractGenerator(Project project) {
        this.project = project;
        name = getClass().getSimpleName();
    }

    /// Sets the name of the generator.
    ///
    /// @param <G> the generator's type
    /// @param name the name
    /// @return the generator
    ///
    @SuppressWarnings("unchecked")
    public <G extends AbstractGenerator<T>> G name(String name) {
        this.name = name;
        return (G) this;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Project project() {
        return project;
    }

}
