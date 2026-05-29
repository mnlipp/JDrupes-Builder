package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import static org.jdrupes.builder.api.CoreProperties.*;
import static org.jdrupes.builder.mvnrepo.MvnProperties.GroupId;
import static org.jdrupes.builder.mvnrepo.MvnProperties.ArtifactId;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;
import java.nio.file.Path;
import java.util.Map;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.Javadoc;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.LibraryBuilder;
import org.jdrupes.builder.mvnrepo.JavadocJarBuilder;
import org.jdrupes.builder.mvnrepo.MvnPublisher;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.jdrupes.builder.mvnrepo.SourcesJarBuilder;

public class NodeJs extends AbstractProject
        implements JavaProject, JdbldExtension {

    public NodeJs() {
        super(name("nodejs"));
        set(ArtifactId, "jdbld-ext-nodejs");
        dependency(Reveal, project(Root.class));
        generator(PomFileGenerator::new).adaptPom(Root.addCommonPomInfo());
        generator(LibraryBuilder::new)
            .addFrom(providers().select(Supply))
            .addEntries(resources(of(PomFileType).using(Supply))
                .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                    .resolve((String) get(GroupId)).resolve(name())
                    .resolve("pom.xml"), pomFile)))
            .jarName(get(ArtifactId) + "-" + get(Version) + ".jar");

        // Publication
        generator(SourcesJarBuilder::new).addTrees(
            resources(of(JavaSourceTreeType).using(Supply, Expose)));
        generator(Javadoc::new).options("-quiet");
        generator(JavadocJarBuilder::new);
        generator(MvnPublisher::new);
    }

}
