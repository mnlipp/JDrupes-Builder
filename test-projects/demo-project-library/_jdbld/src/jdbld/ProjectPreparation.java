/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
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

import static java.util.jar.Attributes.Name.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.*;
import org.jdrupes.builder.api.*;
import static org.jdrupes.builder.api.Intent.*;
import static org.jdrupes.builder.api.Project.Properties.*;
import org.jdrupes.builder.eclipse.EclipseConfigurator;
import org.jdrupes.builder.java.*;
import org.jdrupes.builder.junit.JUnitTestRunner;
import org.jdrupes.builder.mvnrepo.*;
import static org.jdrupes.builder.mvnrepo.MvnProperties.*;

public class ProjectPreparation {

    public static void setupCommonGenerators(Project project) {
        if (project instanceof JavaProject) {
            if (project instanceof MergedTestProject) {
                setupTestProject(project);
            } else {
                setupProject(project);
            }
        }
        if (project instanceof JavaLibraryProject) {
            // Generate POM
            project.generator(PomFileGenerator::new).adaptPom(model -> {
                model.setDescription("See URL.");
                model.setUrl("http://mnlipp.github.io/jgrapes/");
                var scm = new Scm();
                scm.setUrl("scm:git@github.com:mnlipp/jgrapes.git");
                scm.setConnection(
                    "scm:git@github.com:mnlipp/jgrapes.git");
                scm.setDeveloperConnection(
                    "git@github.com:mnlipp/jgrapes.git");
                model.setScm(scm);
                var license = new License();
                license.setName("AGPL 3.0");
                license.setUrl("https://www.gnu.org/licenses/agpl-3.0.en.html");
                license.setDistribution("repo");
                model.setLicenses(List.of(license));
                var developer = new Developer();
                developer.setId("mnlipp");
                developer.setName("Michael N. Lipp");
                model.setDevelopers(List.of(developer));
            });

            project.generator(LibraryGenerator::new)
                .from(project.providers().select(Supply))
                .attributes(Map.of(
                    IMPLEMENTATION_TITLE, project.name(),
                    IMPLEMENTATION_VERSION, project.get(Version),
                    IMPLEMENTATION_VENDOR, "Michael N. Lipp (mnl@mnl.de)")
                    .entrySet().stream())
                .addEntries(project.resources(project
                    .of(PomFile.class).using(Supply))
                    .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                        .resolve((String) project.get(GroupId))
                        .resolve(project.name())
                        .resolve("pom.xml"), pomFile)));
        }

    }

    private static void setupProject(Project project) {
        project.generator(JavaCompiler::new).addSources(Path.of("src"),
            "**/*.java");
        project.generator(JavaResourceCollector::new).add(
            Path.of("resources"), "**/*");
    }

    private static void setupTestProject(Project project) {
        project.generator(JavaCompiler::new).addSources(Path.of("test"),
            "**/*.java");
        project.generator(JavaResourceCollector::new).add(Path.of(
            "test-resources"), "**/*");
        project.dependency(Consume, new MvnRepoLookup()
            .bom("org.junit:junit-bom:5.12.2")
            .resolve("org.junit.jupiter:junit-jupiter-api")
            .resolve("org.junit.jupiter:junit-jupiter-engine"));
        project.dependency(Supply, JUnitTestRunner::new);
    }

    public static void setupEclipseConfigurator(Project project) {
        project.generator(new EclipseConfigurator(project)
            .eclipseAlias(project instanceof RootProject ? project.name()
                : "org.jdrupes.builder.demo.library." + project.name())
            .adaptProjectConfiguration((doc, buildSpec, natures) -> {
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
            })
            .adaptConfiguration(() -> {
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
}
