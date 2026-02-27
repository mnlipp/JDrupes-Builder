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

import java.util.Objects;
import org.jdrupes.builder.api.ExecResult;
import org.jdrupes.builder.api.FaultAware;
import org.jdrupes.builder.api.ResourceProvider;

/// Default implementation of a test result.
///
public class DefaultExecResult extends ResourceObject implements ExecResult {

    private final ResourceProvider provider;
    private final int exitValue;
    private boolean isFaulty;

    /// Initializes a new default exec result. Note that an `exitValue`
    /// different from 0 does not automatically mark the result as faulty. 
    ///
    /// @param provider the provider
    /// @param name the name
    /// @param exitValue the exit value
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    protected DefaultExecResult(ResourceProvider provider,
            String name, int exitValue) {
        name(name);
        this.provider = provider;
        this.exitValue = exitValue;
    }

    @Override
    public int exitValue() {
        return exitValue;
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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(exitValue, provider);
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
        if (!(obj instanceof DefaultExecResult)) {
            return false;
        }
        DefaultExecResult other = (DefaultExecResult) obj;
        return exitValue == other.exitValue
            && Objects.equals(provider, other.provider);
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        return ExecResult.class.getSimpleName()
            + name().map(n -> ": " + n).orElse("")
            + " [exitValue=" + exitValue + "]";
    }
}
