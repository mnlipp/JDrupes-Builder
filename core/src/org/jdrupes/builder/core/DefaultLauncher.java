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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.LogManager;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;

/// A default implementation of a [Launcher].
///
public class DefaultLauncher implements Launcher {

    private final Project rootProject;

    /// Instantiates a new default launcher.
    ///
    /// @param project the project
    ///
    public DefaultLauncher(Project project) {
        rootProject = project;
    }

    @Override
    @SuppressWarnings("PMD.AvoidPrintStackTrace")
    public void start(String[] args) {
        InputStream props;
        try {
            props = Files.newInputStream(Path.of("logging.properties"));
        } catch (IOException e) {
            props = DefaultLauncher.class
                .getResourceAsStream("logging.properties");
        }
        // Get logging properties from file and put them in effect
        try (var from = props) {
            LogManager.getLogManager().readConfiguration(from);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

        // Start building
        rootProject.provide(AnyResource.of(Resource.KIND_APP_JAR)).stream()
            .forEach(r -> {
                System.out.println(r);
            });
    }

}
