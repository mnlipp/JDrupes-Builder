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

package org.jdrupes.builder.startup;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.LauncherSupport;
import org.jdrupes.builder.java.ClasspathElement;
import org.jdrupes.builder.java.CompilationResources;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

/// A default implementation of a [Launcher]. The launcher first builds
/// the user's JDrupes Builder project, using the JDrupes Builder project
/// defined by [BootstrapRoot] and [BootstrapBuild]. The default action
/// of [BootstrapRoot] adds the results from the bootstrap build 
/// to the classpath and launches the actual JDrupes Builder project.
///
public class BootstrapLauncher extends AbstractLauncher {

    /// The log.
    protected final Logger log = Logger.getLogger(getClass().getName());
    private RootProject rootProject;

    /// Instantiates a new bootstrap launcher. An instance of the class
    /// passed as argument is created and used as root project for the
    /// build.
    /// 
    /// Unless the root project is the only project, the root project
    /// must declare dependencies, else the subprojects won't be
    /// instantiated.
    ///
    /// @param rootPrjCls the root project
    /// @param args the args
    ///
    @SuppressWarnings("PMD.UseVarargs")
    public BootstrapLauncher(
            Class<? extends RootProject> rootPrjCls, String[] args) {
        super(args);
        unwrapBuildException(() -> {
            rootProject = LauncherSupport.createProjects(
                rootPrjCls, Collections.emptyList(), jdbldProps, commandLine);

            // Add build extensions to the build project.
            var mvnLookup = new MvnRepoLookup();
            Arrays.asList(jdbldProps
                .getProperty(BootstrapBuild.BUILD_EXTENSIONS, "").split(","))
                .stream().map(String::trim).filter(c -> !c.isBlank())
                .forEach(mvnLookup::resolve);
            ((Project) rootProject.project(BootstrapBuild.class))
                .dependency(Expose, mvnLookup);
            @SuppressWarnings("PMD.UseDiamondOperator")
            var cpUrls = rootProject.get(
                new ResourceRequest<ClasspathElement>(
                    new ResourceType<CompilationResources>() {}))
                .map(cpe -> {
                    try {
                        if (cpe instanceof FileTree tree) {
                            return tree.root().toFile().toURI().toURL();
                        }
                        return ((FileResource) cpe).path().toFile().toURI()
                            .toURL();
                    } catch (MalformedURLException e) {
                        // Cannot happen
                        throw new BuildException(e);
                    }
                }).toArray(URL[]::new);
            new DirectLauncher(new URLClassLoader(cpUrls,
                Thread.currentThread().getContextClassLoader()), args);
            return null;
        });
    }

    @Override
    public <T extends Resource> Stream<T> provide(ResourceRequest<T> request) {
        return unwrapBuildException(() -> {
            // Provide requested resource, handling all exceptions here
            var result = rootProject.get(request).toList();
            return result.stream();
        });
    }

    /// The main method.
    ///
    /// @param args the arguments
    ///
    public static void main(String[] args) {
        new BootstrapLauncher(BootstrapRoot.class, args);
    }
}
