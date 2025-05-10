package org.jdrupes.builder.core;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

import org.jdrupes.builder.api.Resource;

public class FileResource implements Resource {

    private final Path path;

    public FileResource(Path path) {
        this.path = path;
    }

    public Path path() {
        return path;
    }

    public Instant asOf() {
        return Instant.ofEpochMilli(path.toFile().lastModified());
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
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
        FileResource other = (FileResource) obj;
        return Objects.equals(path, other.path);
    }

    @Override
    public String toString() {
        return "File: " + path.toString() + " (" + asOf() + ")";
    }
}
