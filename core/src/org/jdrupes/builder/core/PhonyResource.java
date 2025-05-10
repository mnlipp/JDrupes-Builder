package org.jdrupes.builder.core;

import java.util.Objects;
import org.jdrupes.builder.api.Resource;

public class PhonyResource implements Resource {

    private String name;

    public PhonyResource(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PhonyResource other = (PhonyResource) obj;
        return Objects.equals(name, other.name);
    }
}
