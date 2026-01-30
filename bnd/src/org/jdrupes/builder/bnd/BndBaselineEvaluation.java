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
import org.jdrupes.builder.api.FaultAware;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;

/// Provides the results from a baseline evaluation.
///
public interface BndBaselineEvaluation extends Resource, FaultAware {

    /// The project.
    ///
    /// @return the project
    ///
    Project project();

    /// The bundle symbolic name name of the baselined jar.
    ///
    /// @return the name
    ///
    @Override
    Optional<String> name();

    /// The path of the baselined jar.
    ///
    /// @return the path
    ///
    Path bundlePath();

    /// If set, indicates that baselining failed because the artifact
    /// to use as baseline was not found.
    ///
    /// @return true, if successful
    ///
    boolean baselineArtifactMissing();

    /// If set, indicates that a mismatch was detected.
    ///
    /// @return true, if successful
    ///
    boolean mismatch();

    /// If there is a mismatch, the reason for
    /// the mismatch.
    ///
    /// @return the reason
    ///
    Optional<String> reason();

    /// The location where the report can be found.
    ///
    /// @return the path
    ///
    Path reportLocation();
}
