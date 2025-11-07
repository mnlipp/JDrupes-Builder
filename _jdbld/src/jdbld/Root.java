package jdbld;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Scm;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Project;
import static org.jdrupes.builder.api.Project.Properties.*;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceRequest.requestFor;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.mvnrepo.JavadocJarGenerator;
import org.jdrupes.builder.mvnrepo.MvnPublication;
import org.jdrupes.builder.mvnrepo.MvnPublisher;
import org.jdrupes.builder.mvnrepo.MvnRepoDependency.Scope;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.mvnrepo.PomFile;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.jdrupes.builder.mvnrepo.SourcesJarGenerator;
import org.jdrupes.builder.uberjar.UberJarGenerator;
import org.jdrupes.builder.vscode.VscodeConfiguration;
import static org.jdrupes.builder.mvnrepo.MvnProperties.*;
import org.jdrupes.builder.java.Javadoc;
import org.jdrupes.builder.java.JavadocDirectory;
import org.jdrupes.builder.java.JavadocJarFile;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.SourcesJarFile;
import org.jdrupes.builder.java.JavaSourceFile;

public class Root extends AbstractProject implements RootProject {

    @Override
    public void prepareProject(Project project) {
        ProjectPreparation.setupCommonGenerators(project);
        ProjectPreparation.setupEclipseConfigurator(project);
        ProjectPreparation.setupVscodeConfiguration(project);
    }

    public Root() {
        super(name("jdrupes-builder"));
        set(GroupId, "org.jdrupes");
        set(Version, "0.0.5");

        dependency(Expose, project(Api.class));
        dependency(Expose, project(Core.class));
        dependency(Expose, project(Java.class));
        dependency(Expose, project(MvnRepo.class));
        dependency(Expose, project(Uberjar.class));
        dependency(Expose, project(Startup.class));
        dependency(Expose, project(Eclipse.class));
        dependency(Expose, project(Vscode.class));

        // Generate POM
        dependency(Consume, PomFileGenerator::new).adaptPom(model -> {
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
        });

        // Provide app jar
        dependency(Supply, new UberJarGenerator(this)
            .from(providers(Expose))
            .from(new MvnRepoLookup().resolve(Scope.Runtime,
                "eu.maveniverse.maven.mima.runtime:standalone-static:2.4.34",
                "org.slf4j:slf4j-api:2.0.17",
                "org.slf4j:slf4j-jdk14:2.0.17"))
            .mainClass("org.jdrupes.builder.startup.BootstrapLauncher")
            .addEntries(from(Consume)
                .get(new ResourceRequest<PomFile>(new ResourceType<>() {}))
                .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                    .resolve((String) get(GroupId)).resolve(name())
                    .resolve("pom.xml"), pomFile)))
            .destination(buildDirectory().resolve(Path.of("app"))));

        // Supply javadoc
        generator(Javadoc::new)
            .destination(rootProject().directory().resolve("webpages/javadoc"))
            .tagletpath(from(new MvnRepoLookup()
                .resolve("org.jdrupes.taglets:plantuml-taglet:3.1.0",
                    "net.sourceforge.plantuml:plantuml:1.2023.11"))
                        .get(requestFor(RuntimeClasspathType)))
            .taglets(Stream.of("org.jdrupes.taglets.plantUml.PlantUml",
                "org.jdrupes.taglets.plantUml.StartUml",
                "org.jdrupes.taglets.plantUml.EndUml"))
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

        // Supply sources jar
        generator(SourcesJarGenerator::new)
            .addTrees(get(new ResourceRequest<FileTree<JavaSourceFile>>(
                new ResourceType<>() {})));

        // Supply javadoc jar
        generator(JavadocJarGenerator::new);

        // Publish (deploy). Credentials and signing information is
        // obtained through properties.
        generator(MvnPublisher::new);

        // Commands
        commandAlias("build", requestFor(AppJarFile.class),
            requestFor(JavadocDirectory.class));
        commandAlias("sourcesJar", requestFor(SourcesJarFile.class));
        commandAlias("javadoc", requestFor(JavadocDirectory.class));
        commandAlias("javadocJar", requestFor(JavadocJarFile.class));
        commandAlias("eclipse", requestFor(EclipseConfiguration.class));
        commandAlias("vscode", requestFor(VscodeConfiguration.class));
        commandAlias("pomFile", requestFor(PomFile.class));
        commandAlias("mavenPublication", requestFor(MvnPublication.class));
    }
}
