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
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.eclipse.EclipseConfigurator;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceCollector;
import org.jdrupes.builder.mvnrepo.MavenArtifactProject;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/// The Class ProjectPreparation.
///
public class ProjectPreparation {

    public static void setupCommonGenerators(Project project) {
        if (project instanceof JavaProject) {
            project.generator(JavaCompiler::new)
                .addSources(Path.of("src"), "**/*.java")
                .options("--release", "24");
            project.generator(JavaResourceCollector::new)
                .add(Path.of("resources"), "**/*");
        }
        if (project instanceof MavenArtifactProject) {
            project.generator(new PomFileGenerator(project) {

                @Override
                protected void adaptPom(Document doc, Element project) {
                    project.appendChild(doc.createElement("description"))
                        .setTextContent("See URL.");
                    project.appendChild(doc.createElement("url"))
                        .setTextContent(
                            "https://builder.jdrupes.org/generator-index.html");
                    var scm = project.appendChild(doc.createElement("scm"));
                    scm.appendChild(doc.createElement("url"))
                        .setTextContent(
                            "https://github.com/jdrupes/jdrupes-builder");
                    scm.appendChild(doc.createElement("connection"))
                        .setTextContent(
                            "scm:git://github.com/jdrupes/jdrupes-builder.git");
                    scm.appendChild(doc.createElement("developerConnection"))
                        .setTextContent(
                            "scm:git://github.com/jdrupes/jdrupes-builder.git");
                    var licenses
                        = project.appendChild(doc.createElement("licenses"));
                    var license
                        = licenses.appendChild(doc.createElement("license"));
                    license.appendChild(doc.createElement("name"))
                        .setTextContent("AGPL 3.0");
                    license.appendChild(doc.createElement("url"))
                        .setTextContent(
                            "https://www.gnu.org/licenses/agpl-3.0.en.html");
                    license.appendChild(doc.createElement("distribution"))
                        .setTextContent("repo");
                    var developers
                        = project.appendChild(doc.createElement("developers"));
                    var developer = developers
                        .appendChild(doc.createElement("developer"));
                    developer.appendChild(doc.createElement("name"))
                        .setTextContent("Michael N. Lipp");
                    developer.appendChild(doc.createElement("id"))
                        .setTextContent("mnlipp");
                }

            });
        }
    }

    public static void setupEclipseConfigurator(Project project) {
        project.generator(new EclipseConfigurator(project) {

            @Override
            protected void adaptProjectConfiguration(Document doc,
                    Node buildSpec, Node natures) {
                if (project() instanceof JavaProject) {
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
            }

            @Override
            protected void adaptConfiguration() throws IOException {
                if (!(project() instanceof JavaProject)) {
                    return;
                }
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
            }
        });

    }

}
