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

package org.jdrupes.builder.startup;

import com.google.common.flogger.FluentLogger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ConfigurationException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractRootProject;
import org.jdrupes.builder.core.DefaultBuildContext;
import org.jdrupes.builder.java.ClasspathElement;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

/// An implementation of a [Launcher] that bootstraps the build.
/// The [BootstrapProjectLauncher] uses the built-in [BootstrapRoot] and
/// [BootstrapBuild] to assemble a JDrupes Builder [Project] (the
/// bootstrap project) that includes the [JavaCompiler] for compiling
/// the JDrupes Builder configuration provided by the user. 
/// 
/// The launcher then requests the *supplied* and *exposed* classes from
/// the bootstrap project, including in particular the [RootProject] of
/// the user's build configuration. The launcher uses these classes as
/// classpath for creating the [BuildProjectLauncher]
///
public class BootstrapProjectLauncher extends AbstractLauncher {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    /// The JDrupes Builder properties read from the file
    /// `.jdbld.properties` in the root project.
    protected Properties jdbldProps;
    /// The command line.
    protected CommandLine commandLine;
    private final AbstractRootProject bootstrapProject;
    private final Path buildRootDirectory;

    /// Initializes a new bootstrap launcher.
    ///
    /// @param rootPrjCls the root project class
    /// @param args the arguments
    ///
    @SuppressWarnings("PMD.UseVarargs")
    public BootstrapProjectLauncher(
            Class<? extends RootProject> rootPrjCls, String[] args) {
        buildRootDirectory = Path.of("").toAbsolutePath();
        jdbldProps = propertiesFromFiles(buildRootDirectory);
        try {
            commandLine = new DefaultParser().parse(baseOptions(), args);
        } catch (ParseException e) {
            configureLogging(buildRootDirectory, jdbldProps);
            throw new ConfigurationException().cause(e);
        }
        addCliProperties(jdbldProps, commandLine);
        configureLogging(buildRootDirectory, jdbldProps);

        bootstrapProject = DefaultBuildContext.createProjects(
            buildRootDirectory, rootPrjCls, Collections.emptyList(), jdbldProps,
            commandLine);
    }

    @Override
    public void close() {
        bootstrapProject.close();
    }

    /// Builds the build project launcher.
    ///
    /// @param rootPrjCls the root project
    /// @param args the args
    /// @return the builds the project launcher
    ///
    @SuppressWarnings("PMD.UseVarargs")
    public BuildProjectLauncher buildBuildProjectLauncher(
            Class<? extends RootProject> rootPrjCls, String[] args) {
        return bootstrapProject.context().call(() -> {
            URL[] cpUrls = buildProjectClasses(bootstrapProject);
            logger.atFine().log("Build project launcher with classpath: %s",
                Arrays.toString(cpUrls));
            return new BuildProjectLauncher(
                new URLClassLoader(cpUrls, getClass().getClassLoader()),
                buildRootDirectory, args);
        });
    }

    private URL[] buildProjectClasses(RootProject rootProject) {
        // Add build extensions to the build project.
        var mvnLookup = new MvnRepoLookup();
        Optional.ofNullable(jdbldProps
            .getProperty(BootstrapBuild.EXTENSIONS_SNAPSHOT_REPO, null))
            .map(URI::create).ifPresent(mvnLookup::snapshotRepository);
        var buildCoords = Arrays.asList(jdbldProps
            .getProperty(BootstrapBuild.BUILD_EXTENSIONS, "").split(","))
            .stream().map(String::trim).filter(c -> !c.isBlank()).toList();
        logger.atFine().log("Adding build extensions: %s"
            + " to classpath for builder project compilation", buildCoords);
        buildCoords.forEach(mvnLookup::resolve);
        rootProject.project(BootstrapBuild.class).dependency(Expose,
            mvnLookup);
        return rootProject.resources(rootProject
            .of(ClasspathElement.class).using(Supply, Expose)).map(cpe -> {
                try {
                    if (cpe instanceof FileTree tree) {
                        return tree.root().toFile().toURI().toURL();
                    }
                    return ((FileResource) cpe).path().toFile().toURI()
                        .toURL();
                } catch (MalformedURLException e) {
                    // Cannot happen
                    throw new BuildException().from(rootProject).cause(e);
                }
            }).toArray(URL[]::new);
    }

    /// Root project.
    ///
    /// @return the root project
    ///
    @Override
    public AbstractRootProject rootProject() {
        return bootstrapProject;
    }

    /// The main method.
    ///
    /// @param args the arguments
    ///
    @SuppressWarnings("PMD.SystemPrintln")
    public static void main(String[] args) {
        try {
            if (!reportBuildException(() -> {
                BuildProjectLauncher buildPl;
                try (var bootPl = new BootstrapProjectLauncher(
                    BootstrapRoot.class, args)) {
                    buildPl = bootPl.buildBuildProjectLauncher(
                        BootstrapRoot.class, args);
                }
                try (buildPl) {
                    return buildPl.runCommands();
                }
            })) {
                System.exit(1);
            }
        } catch (BuildException e) {
            if (e.getCause() == null) {
                logger.atSevere().log("Build failed: %s",
                    formatter().summary(e));
            } else {
                logger.atSevere().withCause(e).log("Build failed: %s",
                    formatter().summary(e));
            }
            System.out.println(formatter().summary(e));
            if (!e.details().isBlank()) {
                System.out.println(e.details());
            }
            System.exit(2);
        }
    }
}
