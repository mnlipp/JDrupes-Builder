package jdbld;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Project;
import static org.jdrupes.builder.api.Project.Properties.*;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.java.ClasspathElement;
import org.jdrupes.builder.java.JarGenerator;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.mvnrepo.MvnPublication;
import org.jdrupes.builder.mvnrepo.MvnPublicationGenerator;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.mvnrepo.PomFile;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.jdrupes.builder.uberjar.UberJarGenerator;
import static org.jdrupes.builder.mvnrepo.MvnProperties.*;
import org.jdrupes.builder.java.Javadoc;
import org.jdrupes.builder.java.JavadocDirectory;
import org.jdrupes.builder.java.SourcesJarFile;
import org.jdrupes.builder.java.JavaSourceFile;

public class Root extends AbstractProject implements RootProject {

    @Override
    public void prepareProject(Project project) {
        ProjectPreparation.setupCommonGenerators(project);
        ProjectPreparation.setupEclipseConfigurator(project);
    }

    public Root() {
        super(name("jdrupes-builder"));
        set(GroupId, "org.jdrupes");
        set(Version, "0.0.3-SNAPSHOT");

        dependency(Expose, project(Api.class));
        dependency(Expose, project(Core.class));
        dependency(Expose, project(Java.class));
        dependency(Expose, project(MvnRepo.class));
        dependency(Expose, project(Uberjar.class));
        dependency(Expose, project(Startup.class));
        dependency(Expose, project(Eclipse.class));

        // Generate POM
        generator(new PomFileGenerator(this) {

            @Override
            protected void adaptPom(Model model) {
                model.setDescription("See URL.");
                model.setUrl("https://builder.jdrupes.org/");
                var scm = new Scm();
                scm.setUrl("https://github.com/jdrupes/jdrupes-builder");
                scm.setConnection(
                    "scm:git://github.com/jdrupes/jdrupes-builder.git");
                scm.setDeveloperConnection(
                    "scm:git://github.com/jdrupes/jdrupes-builder.git");
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
            }
        });

        // Build app jar
        dependency(Forward, new UberJarGenerator(this)
            .from(providers(Expose))
            .from(new MvnRepoLookup(this).artifact(
                "eu.maveniverse.maven.mima.runtime:standalone-static:2.4.34")
                .artifact("org.slf4j:slf4j-api:2.0.17")
                .artifact("org.slf4j:slf4j-jdk14:2.0.17"))
            .mainClass("org.jdrupes.builder.startup.BootstrapLauncher")
            .addEntries(
                supplied(new ResourceRequest<PomFile>(new ResourceType<>() {}))
                    .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                        .resolve((String) get(GroupId)).resolve(name())
                        .resolve("pom.xml"), pomFile)))
            .destination(directory().resolve(Path.of("_jdbld", "app"))));

        // Build sources jar
        generator(new JarGenerator(this, SourcesJarFileType)
            .addTrees(get(new ResourceRequest<FileTree<JavaSourceFile>>(
                new ResourceType<>() {})))
            .jarName(name() + "-" + get(Version) + "-sources.jar")
            .destination(directory().resolve(Path.of("_jdbld", "app"))));

        // Build javadoc
        generator(Javadoc::new)
            .destination(rootProject().directory().resolve("webpages/javadoc"))
            .tagletpath(from(new MvnRepoLookup(this)
                .artifact("org.jdrupes.taglets:plantuml-taglet:3.1.0")
                .artifact("net.sourceforge.plantuml:plantuml:1.2023.11"))
                    .get(new ResourceRequest<ClasspathElement>(
                        RuntimeResourcesType)))
            .taglets(Stream.of("org.jdrupes.taglets.plantUml.PlantUml",
                "org.jdrupes.taglets.plantUml.StartUml",
                "org.jdrupes.taglets.plantUml.EndUml"))
            .addSources(get(new ResourceRequest<FileTree<JavaSourceFile>>(
                new ResourceType<>() {})))
            .options("-overview", directory().resolve("overview.md").toString())
            .options("--add-stylesheet",
                directory().resolve("misc/javadoc-overwrites.css").toString())
            .options("--add-script",
                directory().resolve("misc/highlight.min.js").toString())
            .options("--add-script",
                directory().resolve("misc/highlight-all.js").toString())
            .options("--add-stylesheet",
                directory().resolve("misc/highlight-default.css").toString())
            .options("-bottom",
                readString(directory().resolve("misc/javadoc.bottom.txt")))
            .options("--allow-script-in-comments")
            .options("-linksource")
            .options("-link",
                "https://docs.oracle.com/en/java/javase/25/docs/api/")
            .options("-link",
                "https://maven.apache.org/ref/3-LATEST/apidocs/")
            .options("-quiet");

        // Publish (deploy)
        generator(MvnPublicationGenerator::new)
            .snapshotRepository(URI.create(
                "https://central.sonatype.com/repository/maven-snapshots/"))
            .credentials(
                context().property("cscuser"), context().property("cscpass"));

        // Commands
        commandAlias("build",
            new ResourceRequest<AppJarFile>(new ResourceType<>() {}),
            new ResourceRequest<JavadocDirectory>(
                new ResourceType<>() {}));
        commandAlias("sources",
            new ResourceRequest<SourcesJarFile>(new ResourceType<>() {}));
        commandAlias("javadoc",
            new ResourceRequest<JavadocDirectory>(
                new ResourceType<>() {}));
        commandAlias("eclipse",
            new ResourceRequest<EclipseConfiguration>(new ResourceType<>() {}));
        commandAlias("pomFile",
            new ResourceRequest<PomFile>(new ResourceType<>() {}));
        commandAlias("mavenPublication",
            new ResourceRequest<MvnPublication>(new ResourceType<>() {}));
    }
}
