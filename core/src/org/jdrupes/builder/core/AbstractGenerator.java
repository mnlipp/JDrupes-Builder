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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.CleanlinessType;

// TODO: Auto-generated Javadoc
/// A base implementation of a [Generator].
///
public abstract class AbstractGenerator extends AbstractProvider
        implements Generator {

    private final Project project;
    private String name;

    /// Instantiates a new abstract generator.
    ///
    /// @param project the project
    ///
    public AbstractGenerator(Project project) {
        this.project = project;
        name = getClass().getSimpleName();
        if (name.isBlank()) {
            name = "Adapted " + getClass().getSuperclass().getSimpleName();
        }
    }

    /// Sets the name of the generator.
    ///
    /// @param name the name
    /// @return the generator
    ///
    public AbstractGenerator name(String name) {
        this.name = name;
        return this;
    }

    /// Name.
    ///
    /// @return the string
    ///
    @Override
    public String name() {
        return name;
    }

    /// Project.
    ///
    /// @return the project
    ///
    @Override
    public final Project project() {
        return project;
    }

    /// Short for `project().newResource(type, args)`.
    ///
    /// @param <T> the generic type
    /// @param type the type
    /// @param args the args
    /// @return the t
    ///
    protected <T extends Resource> T newResource(ResourceType<T> type,
            Object... args) {
        return project.newResource(type, args);
    }

    /// If the request includes [Cleanliness] deletes the given files 
    /// and returns `true`.
    ///
    /// @param requested the requested resource
    /// @param files the files
    /// @return true, if successful
    ///
    protected boolean cleanup(ResourceRequest<?> requested, Path... files) {
        if (!requested.includes(CleanlinessType)) {
            return false;
        }
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warning(() -> file + " cannot be deleted.");
            }
        }
        return true;
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        return name + " in project " + project().name();
    }

}
