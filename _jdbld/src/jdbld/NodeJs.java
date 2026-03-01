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
import org.jdrupes.builder.mvnrepo.PomFile;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.jdrupes.builder.mvnrepo.SourcesJarGenerator;

public class NodeJs extends AbstractProject
        implements JavaProject, JdbldExtension {

    public NodeJs() {
        super(name("nodejs"));
        set(ArtifactId, "jdbld-ext-nodejs");
        dependency(Reveal, project(Root.class));
        generator(PomFileGenerator::new).adaptPom(Root.addCommonPomInfo());
        generator(LibraryBuilder::new)
            .addFrom(providers().select(Supply))
            .addEntries(resources(of(PomFile.class).using(Supply))
                .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                    .resolve((String) get(GroupId)).resolve(name())
                    .resolve("pom.xml"), pomFile)))
            .jarName((String) get(ArtifactId) + "-" + get(Version) + ".jar");
        dependency(Reveal, new MvnRepoLookup()
            .resolve("org.apache.commons:commons-compress:1.28.0"));

        // Publication
        generator(SourcesJarGenerator::new).addTrees(
            resources(of(JavaSourceTreeType).using(Supply, Expose)));
        generator(Javadoc::new).options("-quiet");
        generator(JavadocJarBuilder::new);
        generator(MvnPublisher::new);
    }

}
