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

package org.jdrupes.builder.ext.pmd;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.core.ResourceObject;

/// A default implementation of [PmdResult].
///
public class DefaultPmdResult extends ResourceObject
        implements PmdResult {

    private final Project project;
    private final ResourceProvider provider;
    private final long checked;
    private final long violations;

    /// Initializes a new default PMD result.
    ///
    /// @param project the project
    /// @param provider the provider
    /// @param checked the number of checked files
    /// @param violations the number of rule violations
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    protected DefaultPmdResult(Project project, ResourceProvider provider,
            long checked, long violations) {
        super(PmdTypes.PmdResultType);
        name("PmdChecker: " + checked + " checked, "
            + violations + " violations");
        this.project = project;
        this.provider = provider;
        this.checked = checked;
        this.violations = violations;
    }

    @Override
    public Project project() {
        return project;
    }

    @Override
    public long checked() {
        return checked;
    }

    @Override
    public long violations() {
        return violations;
    }

    @Override
    public String toString() {
        return PmdResult.class.getSimpleName() + " from "
            + provider + ": " + checked + " checked, "
            + violations + " violations";
    }
}
