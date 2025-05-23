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
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;

/// A default implementation of a [Launcher].
///
public class DefaultLauncher implements Launcher {

    protected final Logger log = Logger.getLogger(getClass().getName());

    private final Project rootProject;

    @SafeVarargs
    public DefaultLauncher(Class<? extends Project>... projects) {
        try {
            var cls = projects[0];
            rootProject = cls.getConstructor(Class[].class)
                .newInstance((Object) Arrays.copyOfRange(projects, 1,
                    projects.length));
        } catch (NoSuchMethodException | SecurityException
                | NegativeArraySizeException | InstantiationException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    @SuppressWarnings({ "PMD.AvoidPrintStackTrace",
        "PMD.AvoidReassigningCatchVariables", "PMD.DoNotTerminateVM" })
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
        try {
            rootProject.provide(AllResources.of(Resource.KIND_APP_JAR))
                .forEach(r -> {
                    System.out.println(r);
                });
        } catch (BuildException e) {
            var cause = e.getCause();
            while (cause != null) {
                if (cause instanceof BuildException nbe) {
                    e = nbe;
                }
                cause = cause.getCause();
            }
            final var rootCase = e;
            log.severe(() -> "Build failed: " + rootCase.getMessage());
            System.exit(1);
        }
    }

}
