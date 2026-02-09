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

import java.io.StringWriter;
import java.util.Optional;

/// Represents an exception that occurs during the build. Terminates the
/// current build when thrown.
///
@SuppressWarnings("serial")
public class BuildException extends RuntimeException {

    /// The reason the build failed.
    @SuppressWarnings("PMD.FieldNamingConventions")
    public enum Reason {
        /// A requested resource is unavailable, because transforming one
        /// kind of resource into another kind of resource failed due
        /// to an inconsistent state of the resources. It is up to the
        /// provider to emit messages that describe the reason for
        /// the failure. 
        Unavailable,
        
        /// There is a problem with the build configuration. The exception's
        /// message and optional details should provide more information.
        Configuration,
        
        /// There is an unexpected problem with the build process, typically
        /// due to an exception that cannot be handled and which should
        /// be added as [#cause]. The [BuildException] will be reported to
        /// the user with the stack trace.
        Error
    }

    /// The reason.
    private final Reason reason;
    /// The message.
    private String message;
    /// The resource provider.
    private ResourceProvider resourceProvider;
    /// The details.
    private final StringWriter details = new StringWriter();

    /// Initializes a new build exception with its reason set to 
    /// [BuildException.Reason#Error].
    ///
    public BuildException() {
        reason = Reason.Error;
    }

    /// Initializes a new build exception with the given reason.
    ///
    /// @param reason the reason
    ///
    protected BuildException(Reason reason) {
        this.reason = reason;
    }
    
    /// Returns the reason why the build failed.
    ///
    /// @return the reason
    ///
    public Reason reason() {
        return reason;
    }
    
    /// Sets the message of the build exception. As a convenience, any
    /// Throwable arguments will be replaced by their message.
    ///
    /// @param format the format
    /// @param args the args
    /// @return the build exception
    ///
    public BuildException message(String format, Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Throwable) {
                args[i] = ((Throwable) args[i]).getMessage();
            }
        }
        var message = String.format(format, args);
        this.message = message;
        return this;
    }

    @Override
    public String getMessage() {
        return message;
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

    /// Add information to the details.
    ///
    /// @param detail the detail
    /// @return the exception
    ///
    public BuildException detail(String detail) {
        details.append(detail);
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

    /// Return the collected details.
    ///
    /// @return the string
    ///
    public String details() {
        details.flush();
        return details.toString();
    }
}
