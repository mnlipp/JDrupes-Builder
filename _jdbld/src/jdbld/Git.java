package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import static org.jdrupes.builder.api.CoreProperties.*;
import static org.jdrupes.builder.mvnrepo.MvnProperties.*;
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

public class Git extends AbstractProject
        implements JavaProject, JdbldExtension {

    public Git() {
        super(name("git"));
        set(ArtifactId, "jdbld-ext-git");
        dependency(Reveal, project(Root.class));
        generator(PomFileGenerator::new).adaptPom(Root.addCommonPomInfo());
        generator(LibraryBuilder::new).addFrom(this)
            .addEntries(resources(of(PomFileType).using(Supply))
                .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                    .resolve((String) get(GroupId)).resolve(name())
                    .resolve("pom.xml"), pomFile)))
            .jarName((String) get(ArtifactId) + "-" + get(Version) + ".jar");
        dependency(Expose,
            new MvnRepoLookup().addRepositories(get(LookupRepositories))
                .resolve("org.jdrupes.gitversioning:core:0.3.0"));
        dependency(Reveal, new MvnRepoLookup()
            .resolve("com.vdurmont:semver4j:3.1.0"));

        // Publication
        generator(SourcesJarBuilder::new).addTrees(
            resources(of(JavaSourceTreeType).using(Supply, Expose)));
        generator(Javadoc::new).options("-quiet");
        generator(JavadocJarBuilder::new);
        generator(MvnPublisher::new).destinations(get(PublishingDestinations));
    }
}
