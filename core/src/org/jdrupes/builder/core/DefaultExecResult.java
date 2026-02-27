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

package org.jdrupes.builder.core;

import java.util.Objects;
import java.util.stream.Stream;
import org.jdrupes.builder.api.ExecResult;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;

/// Default implementation of a test result.
///
/// @param <T> the generic type
///
public class DefaultExecResult<T extends Resource> extends ResourceObject
        implements ExecResult<T> {

    private final ResourceProvider provider;
    private final int exitValue;
    private boolean isFaulty;
    private Stream<T> resources;

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

    /// Exit value.
    ///
    /// @return the int
    ///
    @Override
    public int exitValue() {
        return exitValue;
    }

    /// Checks if is faulty.
    ///
    /// @return true, if is faulty
    ///
    @Override
    public boolean isFaulty() {
        return isFaulty;
    }

    /// Sets the faulty.
    ///
    /// @return the default exec result
    ///
    @Override
    public DefaultExecResult<T> setFaulty() {
        isFaulty = true;
        return this;
    }

    /// Resources.
    ///
    /// @return the stream
    ///
    @Override
    public Stream<T> resources() {
        return resources;
    }

    /// Sets the resources associated with this result.
    ///
    /// @param resources the resources
    /// @return the default exec result
    ///
    public DefaultExecResult<T> resources(Stream<T> resources) {
        this.resources = resources;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(exitValue, isFaulty, provider);
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
        DefaultExecResult<?> other = (DefaultExecResult<?>) obj;
        return exitValue == other.exitValue && isFaulty == other.isFaulty
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
