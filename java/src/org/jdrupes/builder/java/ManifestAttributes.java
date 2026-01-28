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

package org.jdrupes.builder.java;

import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.ResourceObject;
import static org.jdrupes.builder.java.JavaTypes.ManifestAttributesType;

/// A wrapper around [Attributes] to allow their usage as [Resource]. 
///
public class ManifestAttributes extends Attributes implements Resource {

    private final ResourceObject resourceDelegee;

    /// Initializes a new manifest attributes.
    ///
    public ManifestAttributes() {
        this.resourceDelegee = new ResourceObject(ManifestAttributesType) {};
    }

    @Override
    public ResourceType<?> type() {
        return resourceDelegee.type();
    }

    @Override
    public Optional<String> name() {
        return resourceDelegee.name();
    }

    /// Sets the name of the resource.
    ///
    /// @param name the name
    /// @return the resource object
    ///
    @SuppressWarnings("PMD.LooseCoupling")
    public final ManifestAttributes name(String name) {
        resourceDelegee.name(name);
        return this;
    }

    @Override
    public Object put(Object name, Object value) {
        if (resourceDelegee.isLocked()) {
            throw new IllegalStateException(
                "Attributes may only be set immediately after creation.");
        }
        return super.put(name, value);
    }

    @Override
    public Object remove(Object name) {
        if (resourceDelegee.isLocked()) {
            throw new IllegalStateException(
                "Attributes may only be removed immediately after creation.");
        }
        return super.remove(name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(resourceDelegee);
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
        if (!(obj instanceof ManifestAttributes)) {
            return false;
        }
        @SuppressWarnings("PMD.LooseCoupling")
        ManifestAttributes other = (ManifestAttributes) obj;
        return Objects.equals(resourceDelegee, other.resourceDelegee);
    }

}
