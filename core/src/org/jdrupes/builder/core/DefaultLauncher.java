package org.jdrupes.builder.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.LogManager;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Project;

public class DefaultLauncher implements Launcher {

    private static Project rootProject;

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
        rootProject.provide(new PhonyResource("compile"));
    }

}
