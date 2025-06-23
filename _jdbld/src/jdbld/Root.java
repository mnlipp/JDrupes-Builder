package jdbld;

import static org.jdrupes.builder.api.Intend.*;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.java.UberJarGenerator;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.Javadoc;
import org.jdrupes.builder.java.JavadocDirectory;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceCollector;
import org.jdrupes.builder.java.JavaSourceFile;

import static org.jdrupes.builder.java.JavaTypes.*;

public class Root extends AbstractProject implements RootProject {

    @Override
    public void prepareProject(Project project) {
        if (project instanceof JavaProject) {
            project.generator(JavaCompiler::new)
                .addSources(Path.of("src"), "**/*.java");
            project.generator(JavaResourceCollector::new)
                .add(Path.of("resources"), "**/*");
        }
    }

    public Root() {
        super(name("jdrupes-builder"));

        dependency(project(Api.class), Expose);
        dependency(project(Core.class), Expose);
        dependency(project(Java.class), Expose);
        dependency(project(MvnRepo.class), Expose);
        dependency(project(Startup.class), Expose);
        dependency(project(Eclipse.class), Expose);

        // Build app jar
        generator(UberJarGenerator::new).addAll(providers(Intend.Expose))
            .add(new MvnRepoLookup().artifact(
                "eu.maveniverse.maven.mima.runtime:standalone-static:2.4.29")
                .artifact("commons-logging:commons-logging:1.3.5")
                .artifact("org.slf4j:slf4j-api:2.0.17")
                .artifact("org.slf4j:slf4j-jdk14:2.0.17"))
            .mainClass("org.jdrupes.builder.startup.BootstrapLauncher")
            .destination(directory().resolve(Path.of("_jdbld", "app")));

        // Build javadoc
        generator(Javadoc::new).tagletpath(Stream.of(
            create(JarFileType, directory().resolve(
                Path.of("_jdbld/lib/plantuml-taglet-3.1.0.jar"))),
            create(JarFileType, directory().resolve(
                Path.of("_jdbld/lib/plantuml-1.2023.11.jar")))))
            .taglets(Stream.of("org.jdrupes.taglets.plantUml.PlantUml",
                "org.jdrupes.taglets.plantUml.StartUml",
                "org.jdrupes.taglets.plantUml.EndUml"))
            .addSources(get(this, new ResourceRequest<FileTree<JavaSourceFile>>(
                new ResourceType<>() {})))
            .options("-overview", directory().resolve("overview.md").toString())
            .options("--add-stylesheet",
                directory().resolve("misc/javadoc-overwrites.css").toString())
            .options("--add-script",
                directory().resolve("misc/prism.js").toString())
            .options("--add-stylesheet",
                directory().resolve("misc/prism.css").toString())
            .options("-linksource")
            .options("-link",
                "https://docs.oracle.com/en/java/javase/23/docs/api/")
            .options("-quiet");

        // Commands
        defineCommand("build",
            new ResourceRequest<AppJarFile>(new ResourceType<>() {}),
            new ResourceRequest<JavadocDirectory>(
                new ResourceType<>() {}));
    }
}
