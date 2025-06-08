package jdbld;

import static org.jdrupes.builder.api.Intend.*;

import java.nio.file.Path;
import java.util.stream.Collectors;

import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.ResourcesCollector;
import org.jdrupes.builder.java.AppJarBuilder;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaTypes;
import org.jdrupes.builder.java.Javadoc;
import org.jdrupes.builder.java.JavaProject;

public class Root extends AbstractProject implements RootProject {

    @Override
    public void prepareProject(Project project) {
        if (project instanceof JavaProject) {
            project.generator(JavaCompiler::new)
                .addSources(Path.of("src"), "**/*.java");
            project.generator(ResourcesCollector::new)
                .add(Path.of("resources"), "**/*");
        }
    }

    public Root() {
        name("jdrupes-builder");

        dependency(project(Api.class), Expose);
        dependency(project(Core.class), Expose);
        dependency(project(Java.class), Expose);
        dependency(project(Startup.class), Expose);

        // Build app jar
        generator(AppJarBuilder::new).addAll(providers(Intend.CONTRIBUTORS))
            .mainClass("org.jdrupes.builder.startup.BootstrapLauncher")
            .destination(directory().resolve(Path.of("_jdbld", "app")));

        // Build javadoc
        generator(Javadoc::new).addSources(get(this,
            new ResourceRequest<>(JavaTypes.JavaSourceFiles)));
    }

    public void build() {
        get(this, new ResourceRequest<>(JavaTypes.AppJarFile))
            .forEach(System.out::println);
        get(this, new ResourceRequest<>(JavaTypes.JavadocDirectory))
        .collect(Collectors.toSet()).stream().forEach(System.out::println);
    }
}
