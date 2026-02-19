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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.MergedTestProject;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceType.resourceType;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaResourceTree;

import static org.jdrupes.builder.java.JavaTypes.*;

/// The [VscodeConfigurator] provides the resource [VscodeConfiguration].
/// The configuration consists of the configuration files:
///   * .vscode/settings.json
///   * .vscode/launch.json
///   * .vscode/tasks.json
///   
/// Each generated data structure can be post processed by a corresponding
/// `adapt` method before being written to disk.
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
        var result = (Stream<T>) Stream.of(project().newResource(
            resourceType(VscodeConfiguration.class), project().directory()));
        return result;
    }

    private void generateSettings(Path file) throws IOException {
        @SuppressWarnings({ "PMD.UseConcurrentHashMap" })
        Map<String, Object> settings = new HashMap<>();
        settings.put("java.configuration.updateBuildConfiguration",
            "automatic");

        // TODO Generalize. Currently we assume a Java compiler exists
        // and use it to obtain the output directory for all generators
        project().providers().select(Consume, Reveal, Supply)
            .filter(p -> p instanceof JavaCompiler)
            .map(p -> (JavaCompiler) p).findFirst().ifPresent(jc -> {
                var outputDirectory = project().relativize(jc.destination());
                settings.put("java.project.outputPath",
                    outputDirectory.toString());
                jc.optionArgument("--release", "--target", "-target")
                    .filter(jdkLocations::containsKey)
                    .ifPresent(version -> {
                        @SuppressWarnings("PMD.UseConcurrentHashMap")
                        Map<String, Object> runtime = new HashMap<>();
                        runtime.put("name", "JavaSE-" + version);
                        runtime.put("path",
                            jdkLocations.get(version).toString());
                        runtime.put("default", true);
                        settings.put("java.configuration.runtimes",
                            List.of(runtime));
                    });
            });

        // Add project's source trees
        var sourcePaths = new ArrayList<String>();
        addSrcPaths(sourcePaths, project());

        // Add output directories of contributing projects
        var referenced = new ArrayList<String>();
        project().providers().select(Consume, Reveal, Expose, Forward)
            .filter(p -> p instanceof Project)
            .map(Project.class::cast).forEach(p -> {
                if (p instanceof MergedTestProject) {
                    if (p.parentProject().get().equals(project())) {
                        // Test projects contribute their resources to the
                        // parent
                        addSrcPaths(sourcePaths, p);
                    }
                    return;
                }
                addOutputs(referenced, p);
            });

        // Source paths are complete now
        if (!sourcePaths.isEmpty()) {
            settings.put("java.project.sourcePaths", sourcePaths);
        }

        // Add JARs to classpath
        referenced.addAll(project().resources(of(ClasspathElementType)
            .using(Consume, Reveal, Expose)).filter(p -> p instanceof JarFile)
            .map(jf -> ((JarFile) jf).path().toString())
            .collect(Collectors.toList()));
        if (!referenced.isEmpty()) {
            settings.put("java.project.referencedLibraries", referenced);
        }

        // Allow user to adapt settings
        settingsAdaptor.accept(settings);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(settings);
        Files.writeString(file, json);
    }

    private void addSrcPaths(List<String> sourcePaths, Project project) {
        project.providers().without(Project.class).resources(
            of(JavaSourceTreeType).using(Consume, Reveal, Supply))
            .map(FileTree::root).filter(p -> p.toFile().canRead())
            .map(p -> project().relativize(p)).forEach(p -> {
                sourcePaths.add(p.toString());
            });
        project.providers().without(Project.class).resources(
            of(JavaResourceTreeType).using(Consume, Reveal, Supply))
            .map(FileTree::root).filter(p -> p.toFile().canRead())
            .map(p -> project().relativize(p)).forEach(p -> {
                sourcePaths.add(p.toString());
            });
    }

    private void addOutputs(List<String> referenced,
            Project project) {
        // TODO Generalize. Currently we assume a Java compiler exists
        // and use it to obtain the output directory for all generators
        var javaCompiler = project.providers().select(Consume, Reveal, Supply)
            .filter(p -> p instanceof JavaCompiler)
            .map(JavaCompiler.class::cast).findFirst();
        // Output from referenced project becomes input for our project
        var outputDirectory
            = javaCompiler.map(jc -> project().relativize(jc.destination()));
        outputDirectory.ifPresent(o -> {
            referenced.add(o.toString());
        });
    }

    private void generateLaunch(Path file) throws IOException {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> launch = new HashMap<>();
        launch.put("version", "0.2.0");
        launch.put("configurations", new ArrayList<>());
        launchAdaptor.accept(launch);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(launch);
        Files.writeString(file, json);
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
        tasks.put("tasks", new java.util.ArrayList<>());
        tasksAdaptor.accept(tasks);
        ObjectMapper mapper = new ObjectMapper();
        String json
            = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tasks);
        Files.writeString(file, json);
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
