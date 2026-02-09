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

package org.jdrupes.builder.core;

import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.BuildException.Reason;

/// The Class BuildExceptionFormatter.
///
public class DefaultBuildExceptionFormatter implements BuildExceptionFormatter {

    /// Initializes a new default build exception formatter.
    ///
    public DefaultBuildExceptionFormatter() {
        // Make javadoc happy
    }

    @Override
    public String summary(BuildException exc) {
        if (exc.reason() == Reason.Unavailable) {
            return "Build failed";
        }
        StringBuilder text = new StringBuilder(100);
        exc.origin().ifPresent(p -> {
            text.append(p.toString());
        });
        if (text.length() > 0) {
            text.append(": ");
        }
        if (exc.getMessage() != null) {
            text.append(exc.getMessage());
        } else if (exc.getCause() != null
            && exc.getCause().getMessage() != null) {
            text.append(exc.getCause().getMessage());
        } else {
            switch (exc.reason()) {
            case Unavailable:
                text.append("failed");
                break;
            case Configuration:
                text.append("problem with build configuration");
                break;
            default:
                text.append("unspecified problem");
            }
        }
        return text.toString();
    }
}
