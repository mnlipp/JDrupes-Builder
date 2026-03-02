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

package org.jdrupes.builder.vscode;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceType.resourceType;
import org.jdrupes.builder.core.AbstractGenerator;

/// The [VscodeConfigurator] provides the resource [VscodeConfiguration].
/// The configuration consists of the configuration files:
///   * .vscode/settings.json
///   * .vscode/launch.json
///   * .vscode/tasks.json
///   
/// Each generated data structure can be post processed by a corresponding
/// `adapt` method before being written to disk.
/// 
/// VS Code relies on the `.project` and `.classpath` files as used by
/// eclipse for its java support. Currently, the configurator does not
/// generate these files. Use the eclipse configurator in addition.
/// 
public class VscodeConfigurator extends AbstractGenerator {
    @SuppressWarnings({ "PMD.UseConcurrentHashMap",
        "PMD.AvoidDuplicateLiterals" })
    private final Map<String, Path> jdkLocations = new HashMap<>();
    private Consumer<Map<String, Object>> settingsAdaptor = _ -> {
    };
    private Consumer<Map<String, Object>> launchAdaptor = _ -> {
    };
    private Consumer<Map<String, Object>> tasksAdaptor = _ -> {
    };
    private Runnable configurationAdaptor = () -> {
    };

    /// Initializes a new vscode configurator.
    ///
    /// @param project the project
    ///
    public VscodeConfigurator(Project project) {
        super(project);
    }

    /**
     * Allow the user to adapt the settings data structure before writing.
     *
     * @param adaptor the adaptor
     * @return the vscode configurator
     */
    public VscodeConfigurator
            adaptSettings(Consumer<Map<String, Object>> adaptor) {
        settingsAdaptor = adaptor;
        return this;
    }

    /// VSCode does not have a central JDK registry. JDKs can therefore
    /// be configured with this method. 
    ///
    /// @param version the version
    /// @param location the location
    /// @return the vscode configurator
    ///
    public VscodeConfigurator jdk(String version, Path location) {
        jdkLocations.put(version, location);
        return this;
    }

    @Override
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.accepts(resourceType(VscodeConfiguration.class))) {
            return Stream.empty();
        }

        Path vscodeDir = project().directory().resolve(".vscode");
        vscodeDir.toFile().mkdirs();
        try {
            generateSettings(vscodeDir.resolve("settings.json"));
            generateLaunch(vscodeDir.resolve("launch.json"));
            generateTasks(vscodeDir.resolve("tasks.json"));
        } catch (IOException e) {
            throw new BuildException().from(this).cause(e);
        }

        // General overrides
        configurationAdaptor.run();

        // Return a result
        @SuppressWarnings({ "unchecked" })
        var result = (Stream<T>) Stream.of(VscodeConfiguration.from(project()));
        return result;
    }

    private void generateSettings(Path file) throws IOException {
        @SuppressWarnings({ "PMD.UseConcurrentHashMap" })
        Map<String, Object> settings = new HashMap<>();
        settings.put("java.configuration.updateBuildConfiguration",
            "automatic");

        // Allow user to adapt settings
        settingsAdaptor.accept(settings);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(settings);
        Files.writeString(file, json);
    }

    private void generateLaunch(Path file) throws IOException {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> launch = new HashMap<>();
        launch.put("version", "0.2.0");
        launch.put("configurations", new ArrayList<>());
        launchAdaptor.accept(launch);
        if (!((List<?>) launch.get("configurations")).isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(launch);
            Files.writeString(file, json);
        }
    }

    /**
     * Allow the user to adapt the launch data structure before writing.
     * The version and an empty list for "configurations" has already be
     * added.
     *
     * @param adaptor the adaptor
     * @return the vscode configurator
     */
    public VscodeConfigurator
            adaptLaunch(Consumer<Map<String, Object>> adaptor) {
        launchAdaptor = adaptor;
        return this;
    }

    private void generateTasks(Path file) throws IOException {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> tasks = new HashMap<>();
        tasks.put("version", "2.0.0");
        tasks.put("tasks", new ArrayList<>());
        tasksAdaptor.accept(tasks);
        if (!((List<?>) tasks.get("tasks")).isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            String json
                = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(tasks);
            Files.writeString(file, json);
        }
    }

    /**
     * Allow the user to adapt the tasks data structure before writing.
     *
     * @param adaptor the adaptor
     * @return the vscode configurator
     */
    public VscodeConfigurator
            adaptTasks(Consumer<Map<String, Object>> adaptor) {
        tasksAdaptor = adaptor;
        return this;
    }

    /// Allow the user to add additional resources.
    ///
    /// @param adaptor the adaptor
    /// @return the eclipse configurator
    ///
    public VscodeConfigurator adaptConfiguration(Runnable adaptor) {
        configurationAdaptor = adaptor;
        return this;
    }
}
