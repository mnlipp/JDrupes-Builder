package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import static org.jdrupes.builder.mvnrepo.MvnProperties.*;

import java.nio.file.Path;
import java.util.Map;

import static org.jdrupes.builder.api.Project.Properties.Version;
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
import org.jdrupes.builder.mvnrepo.SourcesJarGenerator;

public class Bnd extends AbstractProject
        implements JavaProject, JdbldExtension {

    public Bnd() {
        super(name("bnd"));
        set(ArtifactId, "jdbld-ext-bnd");
        dependency(Reveal, project(Root.class));
        generator(PomFileGenerator::new).adaptPom(Root.addCommonPomInfo());
        generator(LibraryBuilder::new)
            .addFrom(providers().select(Supply))
            .addEntries(resources(of(PomFileType).using(Supply))
                .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                    .resolve((String) get(GroupId)).resolve(name())
                    .resolve("pom.xml"), pomFile)))
            .jarName((String) get(ArtifactId) + "-" + get(Version) + ".jar");
        dependency(Expose, new MvnRepoLookup()
            .resolve("biz.aQute.bnd:biz.aQute.bnd.maven:7.2.1"));

        // Publication
        generator(SourcesJarGenerator::new).addTrees(
            resources(of(JavaSourceTreeType).using(Supply, Expose)));
        generator(Javadoc::new).options("-quiet");
        generator(JavadocJarBuilder::new);
        generator(MvnPublisher::new);
    }
}
