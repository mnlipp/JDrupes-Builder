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

package org.jdrupes.builder.mvnrepo;

import java.util.Optional;
import org.jdrupes.builder.api.CoreProperties;
import static org.jdrupes.builder.api.CoreProperties.*;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intent.Supply;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.java.JarBuilder;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.JavadocDirectory;
import static org.jdrupes.builder.mvnrepo.MvnProperties.ArtifactId;

/// A special [JarBuilder] that generates a JAR with javadoc.
///
///   * The content of the JAR is obtained by requesting resources of
///     type [JavadocDirectory] from the project's suppliers and using
///     each as root for a [FileTree], which is then added to the JAR.
///
///   * The name of the JAR is set to `<artifactId>-<version>-javadoc.jar`,
///     where `<artifactId>` is the value of the project's property
///     [MvnProperties#ArtifactId] with a fallback to the project's name.
///     `<version>` is the value of the project's property
///     [CoreProperties#Version].
///
public class JavadocJarBuilder extends JarBuilder {

    /// Initializes a new javadoc JAR generator.
    ///
    /// @param project the project
    ///
    @SuppressWarnings({ "PMD.ConstructorCallsOverridableMethod" })
    public JavadocJarBuilder(Project project) {
        super(project, JavadocJarFileType);
        var trees = project().resources(
            of(JavadocDirectoryType).using(Supply)).map(
                d -> FileTree.of(project(), d.root(), "**/*"));
        addTrees(trees);
        jarName(Optional.ofNullable(project().get(ArtifactId))
            .orElse(project().name()) + "-" + project().get(Version)
            + "-javadoc.jar");
    }

}
