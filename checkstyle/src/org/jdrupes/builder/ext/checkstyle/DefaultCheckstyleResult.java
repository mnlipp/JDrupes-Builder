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

import org.jdrupes.builder.api.FaultAware;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.core.ResourceObject;

/// A default implementation of [CheckstyleResult].
///
public class DefaultCheckstyleResult extends ResourceObject
        implements CheckstyleResult {

    private final Project project;
    private final ResourceProvider provider;
    private final long checked;
    private final long errors;
    private final long warnings;
    private boolean isFaulty;

    /// Initializes a new default checkstyle result.
    ///
    /// @param project the project
    /// @param provider the provider
    /// @param checked the number of checked files
    /// @param errors the number of style violations with error severity
    /// @param warnings the number of style violations with warning severity
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    protected DefaultCheckstyleResult(Project project,
            ResourceProvider provider, long checked, long errors,
            long warnings) {
        super(CheckstyleTypes.CheckstyleResultType);
        name("CheckstyleChecker: " + checked + " checked, " + errors
            + " errors, " + warnings + " warnings");
        this.project = project;
        this.provider = provider;
        this.checked = checked;
        this.errors = errors;
        this.warnings = warnings;
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
    public long errors() {
        return errors;
    }

    @Override
    public long warnings() {
        return warnings;
    }

    @Override
    public boolean isFaulty() {
        return isFaulty;
    }

    @Override
    public FaultAware setFaulty() {
        isFaulty = true;
        return this;
    }

    @Override
    public String toString() {
        return CheckstyleResult.class.getSimpleName() + " from "
            + provider + ": " + checked + " checked, " + errors + " errors, "
            + warnings + " warnings";
    }
}
