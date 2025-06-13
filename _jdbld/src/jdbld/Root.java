package jdbld;

import static org.jdrupes.builder.api.Intend.*;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.AppJarGenerator;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaTypes;
import org.jdrupes.builder.java.Javadoc;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceCollector;
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

        // Build app jar
        generator(AppJarGenerator::new).addAll(providers(Intend.CONTRIBUTORS))
            .mainClass("org.jdrupes.builder.startup.BootstrapLauncher")
            .destination(directory().resolve(Path.of("_jdbld", "app")));

        // Build javadoc
        generator(Javadoc::new).tagletpath(Stream.of(
            newFileResource(JarFileType, directory().resolve(
                Path.of("_jdbld/lib/plantuml-taglet-3.1.0.jar"))),
            newFileResource(JarFileType, directory().resolve(
                Path.of("_jdbld/lib/plantuml-1.2023.11.jar")))))
            .taglets(Stream.of("org.jdrupes.taglets.plantUml.PlantUml",
                "org.jdrupes.taglets.plantUml.StartUml",
                "org.jdrupes.taglets.plantUml.EndUml"))
            .options("-overview", directory().resolve("overview.md").toString())
            .options("--add-stylesheet",
                directory().resolve("misc/javadoc-overwrites.css").toString())
            .options("--add-script",
                directory().resolve("misc/prism.js").toString())
            .options("--add-stylesheet",
                directory().resolve("misc/prism.css").toString())
            .options("-quiet")

            .addSources(get(this,
                new ResourceRequest<>(JavaTypes.JavaSourceTreeType)));
    }

    public void build() {
        get(this, new ResourceRequest<>(JavaTypes.AppJarFileType))
            .forEach(System.out::println);
        get(this, new ResourceRequest<>(JavaTypes.JavadocDirectoryType))
            .collect(Collectors.toSet()).stream().forEach(System.out::println);
    }
}
