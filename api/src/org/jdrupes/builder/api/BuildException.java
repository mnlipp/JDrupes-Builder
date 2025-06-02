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

package org.jdrupes.builder.api;

/// Represents an exception that occurs during the build. Terminates the
/// current build when thrown.
///
@SuppressWarnings("serial")
public class BuildException extends RuntimeException {

    /// Instantiates a new build exception.
    ///
    /// @param message the message
    /// @param cause the cause
    /// @param enableSuppression the enable suppression
    /// @param writableStackTrace the writable stack trace
    ///
    public BuildException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /// Instantiates a new build exception.
    ///
    /// @param message the message
    /// @param cause the cause
    ///
    public BuildException(String message, Throwable cause) {
        super(message, cause);
    }

    /// Instantiates a new build exception.
    ///
    /// @param message the message
    ///
    public BuildException(String message) {
        super(message);
    }

    /// Instantiates a new build exception.
    ///
    /// @param cause the cause
    ///
    public BuildException(Throwable cause) {
        super(cause);
    }

}
