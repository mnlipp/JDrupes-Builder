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

package org.jdrupes.builder.eclipse;

import java.util.Objects;
import org.jdrupes.builder.api.Intent;
import org.jdrupes.builder.core.DefaultResourceRequest;

/// An implementation of [EclipseLauncherRequest].
///
public class DefaultEclipseLauncherRequest
        extends DefaultResourceRequest<EclipseLauncher>
        implements EclipseLauncherRequest {

    private String mainClass;
    private String arguments;
    private String vmArguments;

    /// Initializes a new default eclipse resource request.
    ///
    protected DefaultEclipseLauncherRequest() {
        super(EclipseTypes.EclipseLauncherType);
    }

    /// Creates a new request for a configuration with the given name.
    /// The request is created with intent [Intent#Supply] because this
    /// is the common usage.
    ///
    /// @param name the name
    /// @return the default eclipse launcher request
    ///
    public static DefaultEclipseLauncherRequest create(String name) {
        return (DefaultEclipseLauncherRequest) new DefaultEclipseLauncherRequest()
            .withName(name).using(Intent.Supply);
    }

    @Override
    public DefaultEclipseLauncherRequest clone() {
        // No special handling of attributes required
        return (DefaultEclipseLauncherRequest) super.clone();
    }

    /// Set the main class for the launch configuration.
    ///
    /// @param mainClass the fully qualified main class name
    /// @return the launcher for method chaining
    ///
    public DefaultEclipseLauncherRequest withMainClass(String mainClass) {
        var result = clone();
        result.mainClass = mainClass;
        return result;
    }

    /// The arguments to use in the launch configuration.
    ///
    /// @param arguments the program arguments
    /// @return the launcher for method chaining
    ///
    public DefaultEclipseLauncherRequest withArguments(String arguments) {
        var result = clone();
        result.arguments = arguments;
        return result;
    }

    /// The VM arguments to use in the launch configuration.
    ///
    /// @param vmArguments the VM arguments
    /// @return the launcher for method chaining
    ///
    public DefaultEclipseLauncherRequest withVmArguments(String vmArguments) {
        var result = clone();
        result.vmArguments = vmArguments;
        return result;
    }

    @Override
    public String mainClass() {
        return mainClass;
    }

    @Override
    public String arguments() {
        return arguments;
    }

    @Override
    public String vmArguments() {
        return vmArguments;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result
            = prime * result + Objects.hash(arguments, mainClass, vmArguments);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof DefaultEclipseLauncherRequest)) {
            return false;
        }
        DefaultEclipseLauncherRequest other
            = (DefaultEclipseLauncherRequest) obj;
        return Objects.equals(arguments, other.arguments)
            && Objects.equals(mainClass, other.mainClass)
            && Objects.equals(vmArguments, other.vmArguments);
    }

}
