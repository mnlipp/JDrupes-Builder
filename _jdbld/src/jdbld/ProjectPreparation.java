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

package jdbld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.eclipse.EclipseConfigurator;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceCollector;
import org.jdrupes.builder.vscode.VscodeConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/// The Class ProjectPreparation.
///
public class ProjectPreparation {

    public static void setupCommonGenerators(Project project) {
        if (project instanceof JavaProject) {
            project.generator(JavaCompiler::new)
                .addSources(Path.of("src"), "**/*.java")
                .options("--release", "25");
            project.generator(JavaResourceCollector::new)
                .add(Path.of("resources"), "**/*");
        }
    }

    public static void setupEclipseConfigurator(Project project) {
        project.generator(new EclipseConfigurator(project)
            .adaptProjectConfiguration((Document doc,
                    Node buildSpec, Node natures) -> {
                if (project instanceof JavaProject) {
                    var cmd = buildSpec
                        .appendChild(doc.createElement("buildCommand"));
                    cmd.appendChild(doc.createElement("name"))
                        .appendChild(doc.createTextNode(
                            "net.sf.eclipsecs.core.CheckstyleBuilder"));
                    cmd.appendChild(doc.createElement("arguments"));
                    natures.appendChild(doc.createElement("nature"))
                        .appendChild(doc.createTextNode(
                            "net.sf.eclipsecs.core.CheckstyleNature"));
                    cmd = buildSpec
                        .appendChild(doc.createElement("buildCommand"));
                    cmd.appendChild(doc.createElement("name"))
                        .appendChild(doc.createTextNode(
                            "ch.acanda.eclipse.pmd.builder.PMDBuilder"));
                    cmd.appendChild(doc.createElement("arguments"));
                    natures.appendChild(doc.createElement("nature"))
                        .appendChild(doc.createTextNode(
                            "ch.acanda.eclipse.pmd.builder.PMDNature"));
                }
            }).adaptConfiguration(() -> {
                if (!(project instanceof JavaProject)) {
                    return;
                }
                try {
                    Files.copy(
                        Root.class.getResourceAsStream("net.sf.jautodoc.prefs"),
                        project.directory()
                            .resolve(".settings/net.sf.jautodoc.prefs"),
                        StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(Root.class.getResourceAsStream("checkstyle"),
                        project.directory().resolve(".checkstyle"),
                        StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(Root.class.getResourceAsStream("eclipse-pmd"),
                        project.directory().resolve(".eclipse-pmd"),
                        StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new BuildException(e);
                }
            }));
    }

    @SuppressWarnings("unchecked")
    public static void setupVscodeConfiguration(Project project) {
        project.generator(VscodeConfigurator::new)
            .jdk("25", Path.of("/usr/lib/jvm/java-25-openjdk/"))
            .adaptLaunch(c -> {
                if (!(project instanceof Root)) {
                    return;
                }
                ((List<Map<String, Object>>) c.get("configurations"))
                    .addAll(List.of(
                        Map.of("type", "java",
                            "name", "Current File",
                            "request", "launch",
                            "mainClass", "${file}",
                            "vmArgs", "-Duser.language=en_US",
                            "args", "build"),
                        Map.of("type", "java",
                            "name", "BootstrapLauncher",
                            "request", "launch",
                            "classPaths", List.of("$Auto",
                                "${workspaceFolder}/eclipse/build/classes",
                                "${workspaceFolder}/uberjar/build/classes",
                                "${workspaceFolder}/vscode/build/classes",
                                "${workspaceFolder}/_jdbld/app/jdrupes-builder-0.0.3-SNAPSHOT.jar"),
                            "mainClass",
                            "org.jdrupes.builder.startup.BootstrapLauncher",
                            "projectName", "startup",
                            "vmArgs", "-Duser.language=en_US",
                            "args", List.of("-B-x", "test-*-project", "build")),
                        Map.of("type", "java",
                            "name", "DirectLauncher",
                            "request", "launch",
                            "classPaths", List.of("$Auto",
                                "${workspaceFolder}/_jdbld/build/classes",
                                "${workspaceFolder}/eclipse/build/classes",
                                "${workspaceFolder}/uberjar/build/classes",
                                "${workspaceFolder}/vscode/build/classes",
                                "${workspaceFolder}/_jdbld/app/jdrupes-builder-0.0.3-SNAPSHOT.jar"),
                            "mainClass",
                            "org.jdrupes.builder.startup.DirectLauncher",
                            "projectName", "startup",
                            "vmArgs", "-Duser.language=en_US",
                            "args", "build")));
            });
    }

}
