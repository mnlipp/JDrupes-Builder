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

import java.nio.file.Path;
import java.util.Optional;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.ResourceObject;

/// Default implementation of a baseline result.
///
public class DefaultBndBaselineEvaluation extends ResourceObject
        implements BndBaselineEvaluation {

    private final Project project;
    private final Path bundlePath;
    private boolean missing;
    private boolean mismatch;
    private boolean faulty;
    private String reason;
    private Path reportLocation;

    /// Initializes a new default bnd baseline result.
    ///
    /// @param type the type
    /// @param project the project
    /// @param bundlePath the bundle path
    ///
    protected DefaultBndBaselineEvaluation(
            ResourceType<?> type, Project project, Path bundlePath) {
        super(type);
        this.project = project;
        this.bundlePath = bundlePath;
    }

    @Override
    public DefaultBndBaselineEvaluation name(String name) {
        super.name(name);
        return this;
    }

    @Override
    public Project project() {
        return project;
    }

    @Override
    public Path bundlePath() {
        return bundlePath;
    }

    @Override
    public boolean mismatch() {
        return mismatch;
    }

    @Override
    public boolean isFaulty() {
        return faulty;
    }

    @Override
    public DefaultBndBaselineEvaluation setFaulty() {
        faulty = true;
        return this;
    }

    /// Sets the reason for a mismatch.
    ///
    /// @param reason the reason
    /// @return the default bnd baseline evaluation
    ///
    public DefaultBndBaselineEvaluation withReason(String reason) {
        this.reason = reason;
        return this;
    }

    @Override
    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    @Override
    public Path reportLocation() {
        return reportLocation;
    }

    /// Sets the report location.
    ///
    /// @param reportLocation the report location
    /// @return the default bnd baseline evaluation
    ///
    public DefaultBndBaselineEvaluation
            withReportLocation(Path reportLocation) {
        this.reportLocation = reportLocation;
        return this;
    }

    /// Sets the baseline artifact missing flag. Also sets the faulty flag
    /// for consistency.
    ///
    /// @return the default bnd baseline evaluation
    ///
    public DefaultBndBaselineEvaluation withBaselineArtifactMissing() {
        missing = true;
        faulty = true;
        return this;
    }

    @Override
    public boolean baselineArtifactMissing() {
        return missing;
    }

    @Override
    public String toString() {
        if (missing) {
            return String.format("BndBaselineEvaluation:%s impossible due"
                + " to missing baseline artifact", name().orElse("(unknown)"),
                project());
        }
        if (mismatch) {
            return String.format("BndBaselineEvaluation:%s failed: %s, see %s",
                name().get(), reason().orElse("(unknown)"),
                project().rootProject().relativize(reportLocation()));
        }
        return String.format("BndBaselineEvaluation:%s succeeded, see %s",
            name().get(), project().rootProject().relativize(reportLocation()));
    }

}
