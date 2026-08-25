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

package org.jdrupes.builder.java;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.jdrupes.builder.api.Resource;

/// Common base interface for resources that can be used in either a
/// classpath or a module-path.
///
public interface CodeContribution extends Resource {

    /// Return the representation of the element as it must appear
    /// on a classpath or module-path.
    ///
    /// @return the path
    ///
    Path toPath();

    /// Checks whether the code contribution is modular. This works for
    /// both JAR files and directories. Both are considered as modular
    /// if they contain a `module-info.class`. JARs are also considered
    /// as modular if they have an `Automatic-Module-Name` set in their
    /// `MANIFEST.MF`.
    /// 
    /// This is provided as static method in order to simplify reuse
    /// in classes implementing this interface.
    ///
    /// @param contribution the contribution
    /// @return true, if the contribution is modular
    ///
    static boolean isModular(CodeContribution contribution) {
        var path = contribution.toPath();
        if (Files.isDirectory(path)) {
            return Files.exists(path.resolve("module-info.class"));
        }
        if (!path.toString().endsWith(".jar")) {
            return false;
        }
        try (var jar = new java.util.jar.JarFile(path.toFile())) {
            var manifest = jar.getManifest();
            if (manifest != null) {
                var name = manifest.getMainAttributes()
                    .getValue("Automatic-Module-Name");
                if (name != null && !name.isBlank()) {
                    return true;
                }
            }
            // This checks for the existence of module-info.class
            // in root and in META-INF/versions
            return ModuleFinder.of(path).findAll().stream()
                .map(ModuleReference::descriptor)
                .anyMatch(d -> !d.isAutomatic());
        } catch (IOException e) { // NOPMD
            // fall through
        }
        return false;
    }

    /// Checks whether this code contribution is modular. This works for
    /// both JAR files and directories. Both are considered as modular
    /// if they contain a `module-info.class`. JARs are also considered
    /// as modular if they have an `Automatic-Module-Name` set in their
    /// `MANIFEST.MF`.
    ///
    /// @return true, if the contribution is modular
    ///
    default boolean isModular() {
        return isModular(this);
    }

    /// Returns the module name if this element is a named module.
    ///
    /// @return the optional module name
    ///
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    default Optional<String> moduleName() {
        // Check first, because ModuleFinder returns a name even for
        // JARs without Automatic-Module-Name
        if (!isModular()) {
            return Optional.empty();
        }
        try {
            var finder = ModuleFinder.of(toPath());
            return finder.findAll().stream()
                .map(m -> m.descriptor().name()).findFirst();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
