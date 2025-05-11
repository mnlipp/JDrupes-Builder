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
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.api.Task;

/// A base implementation of a [Task].
///
/// @param <T> the type of resource in the [Resources] container that
/// this task provides
///
public abstract class AbstractTask<R extends Resource> implements Task<R> {

    protected final Logger log = Logger.getLogger(getClass().getName());

    private final Project project;
    private final String name;

    /// Instantiates a new abstract task.
    ///
    /// @param project the project
    /// @param name the name
    ///
    public AbstractTask(Project project, String name) {
        this.project = project;
        this.name = name;
    }

    /// Instantiates a new abstract task.
    ///
    /// @param project the project
    ///
    public AbstractTask(Project project) {
        this.project = project;
        name = getClass().getSimpleName();
    }

    /// Name.
    ///
    /// @return the string
    ///
    @Override
    public String name() {
        return name;
    }

    /// Project.
    ///
    /// @return the project
    ///
    @Override
    public Project project() {
        return project;
    }
}
