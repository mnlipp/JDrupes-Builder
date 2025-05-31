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
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ClassFile;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceFile;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.DefaultLauncher;

/// The built-in root project associated with the root directory.
///
@SuppressWarnings("PMD.ShortClassName")
public class Root extends AbstractProject implements RootProject {

    /// Instantiates a new root project.
    ///
    public Root() {
        super(BootstrapProject.class);
    }

    /// The main method.
    ///
    /// @param args the arguments
    ///
    public static void main(String[] args) {
        var launcher = new DefaultLauncher(Root.class);
        var cpUrls = Stream.concat(launcher.provide(new ResourceRequest<>(
            new ResourceType<FileTree<ClassFile>>() {
            })),
            launcher.provide(new ResourceRequest<>(
                new ResourceType<FileTree<ResourceFile>>() {
                })))
            .map(ft -> {
                try {
                    return ft.root().toFile().toURI().toURL();
                } catch (MalformedURLException e) {
                    // Cannot happen
                    throw new BuildException(e);
                }
            }).toArray(URL[]::new);
        new DefaultLauncher(new URLClassLoader(cpUrls,
            Thread.currentThread().getContextClassLoader())).start(args);
    }
}
