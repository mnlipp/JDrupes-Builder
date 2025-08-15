package jdbld;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.java.ClasspathElement;
import org.jdrupes.builder.java.UberJarGenerator;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.java.Javadoc;
import org.jdrupes.builder.java.JavadocDirectory;
import org.jdrupes.builder.java.JavaSourceFile;
import static org.jdrupes.builder.java.JavaTypes.*;

public class Root extends AbstractProject implements RootProject {

    @Override
    public void prepareProject(Project project) {
        ProjectPreparation.setupCommonGenerators(project);
        ProjectPreparation.setupEclipseConfigurator(project);
    }

    public Root() {
        super(name("jdrupes-builder"));

        dependency(Expose, project(Api.class));
        dependency(Expose, project(Core.class));
        dependency(Expose, project(Java.class));
        dependency(Expose, project(MvnRepo.class));
        dependency(Expose, project(Startup.class));
        dependency(Expose, project(Eclipse.class));

        // Build app jar
        dependency(Forward, new UberJarGenerator(this).addAll(providers(Expose))
            .add(new MvnRepoLookup().artifact(
                "eu.maveniverse.maven.mima.runtime:standalone-static:2.4.29")
                .artifact("org.slf4j:slf4j-api:2.0.17")
                .artifact("org.slf4j:slf4j-jdk14:2.0.17"))
            .mainClass("org.jdrupes.builder.startup.BootstrapLauncher")
            .destination(directory().resolve(Path.of("_jdbld", "app"))));

        // Build javadoc
        generator(Javadoc::new)
            .destination(rootProject().directory().resolve("webpages/javadoc"))
            .tagletpath(get(new MvnRepoLookup()
                .artifact("org.jdrupes.taglets:plantuml-taglet:3.1.0")
                .artifact("net.sourceforge.plantuml:plantuml:1.2023.11"),
                new ResourceRequest<ClasspathElement>(RuntimeResourcesType)))
            .taglets(Stream.of("org.jdrupes.taglets.plantUml.PlantUml",
                "org.jdrupes.taglets.plantUml.StartUml",
                "org.jdrupes.taglets.plantUml.EndUml"))
            .addSources(get(this, new ResourceRequest<FileTree<JavaSourceFile>>(
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
                "https://docs.oracle.com/en/java/javase/23/docs/api/")
            .options("-quiet");

        // Commands
        commandAlias("build",
            new ResourceRequest<AppJarFile>(new ResourceType<>() {}),
            new ResourceRequest<JavadocDirectory>(
                new ResourceType<>() {}));
        commandAlias("eclipse",
            new ResourceRequest<EclipseConfiguration>(new ResourceType<>() {}));
    }
}
