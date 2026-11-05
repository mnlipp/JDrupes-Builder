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

import static org.jdrupes.builder.api.Intent.*;
import static org.jdrupes.builder.api.CoreProperties.*;
import static org.jdrupes.builder.mvnrepo.MvnProperties.GroupId;
import static org.jdrupes.builder.mvnrepo.MvnProperties.PublishingDestinations;
import static org.jdrupes.builder.mvnrepo.MvnProperties.ArtifactId;
import java.nio.file.Path;
import java.util.Map;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.Javadoc;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.LibraryBuilder;
import org.jdrupes.builder.mvnrepo.JavadocJarBuilder;
import org.jdrupes.builder.mvnrepo.MvnPublisher;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.jdrupes.builder.mvnrepo.SourcesJarBuilder;

public class Pmd extends AbstractProject
        implements JavaProject, JdbldExtension {

    public Pmd() {
        super(name("pmd"));
        set(ArtifactId, "jdbld-ext-pmd");
        // Redundant consume dependencies required for Eclipse configuration
        dependency(Reveal, project(Root.class));
        dependency(Consume, project(Core.class));
        dependency(Consume, project(Java.class));
        generator(PomFileGenerator::new).adaptPom(Root.addCommonPomInfo());
        generator(LibraryBuilder::new).addFrom(this)
            .addEntries(resources(of(PomFileType).using(Supply))
                .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                    .resolve((String) get(GroupId)).resolve(name())
                    .resolve("pom.xml"), pomFile)))
            .jarName((String) get(ArtifactId) + "-" + get(Version) + ".jar");
        // PMD 7.29.0 is not compatible with Saxon 13
        // (see https://github.com/pmd/pmd/issues/6888) but it has no
        // respective version restriction in its dependencies. The "lax"
        // reference to Saxon 12 is upgraded to Saxon 13 by checkstyle's
        // dependencies. So we have to add the restriction here.
        dependency(Expose, new MvnRepoLookup()
            .resolve("net.sourceforge.pmd:pmd-java:7.29.0",
                "net.sf.saxon:Saxon-HE:[0,13.0)"));

        // Publication
        generator(SourcesJarBuilder::new).addTrees(
            resources(of(JavaSourceTreeType).using(Supply, Expose)));
        generator(Javadoc::new).options("-quiet");
        generator(JavadocJarBuilder::new);
        generator(MvnPublisher::new).destinations(get(PublishingDestinations));
    }
}
