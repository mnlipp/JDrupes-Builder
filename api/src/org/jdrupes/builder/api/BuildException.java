/*
 * JDrupes Builder
 * Copyright (C) 2025, 2026 Michael N. Lipp
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

import java.util.Optional;

/// Represents an exception that occurs during the build. Terminates the
/// current build when thrown.
///
@SuppressWarnings("serial")
public class BuildException extends RuntimeException {

    private ResourceProvider resourceProvider;

    /// Initializes a new builds the exception.
    ///
    public BuildException() {
        super();
    }

    /// Instantiates a new build exception. As a convenience, any
    /// Throwable arguments will be replaced by their message.
    ///
    /// @param format the format
    /// @param args the args
    ///
    public BuildException(String format, Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Throwable) {
                args[i] = ((Throwable) args[i]).getMessage();
            }
        }
        var message = String.format(format, args);
        super(message);
    }

    /// Sets the cause.
    ///
    /// @param cause the cause
    /// @return the builds the exception
    ///
    public BuildException cause(Throwable cause) {
        initCause(cause);
        return this;
    }

    /// From.
    ///
    /// @param resourceProvider the resource provider
    /// @return the builds the exception
    ///
    public BuildException from(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
        return this;
    }

    /// Returns the origin of this exception. This is the resource
    /// provider set with [#from].
    ///
    /// @return the resource provider
    ///
    public Optional<ResourceProvider> origin() {
        return Optional.ofNullable(resourceProvider);
    }
}
