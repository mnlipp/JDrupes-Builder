package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.api.Dependency.Intend;
import org.jdrupes.builder.api.ResourceFile;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.ResourcesCollector;
import org.jdrupes.builder.java.JavaCompiler;

public class Startup extends AbstractProject {

    public Startup() {
        name("startup");
        dependency(project(Java.class), Intend.Consume);
        provider(JavaCompiler::new)
            .addSources(newFileTree(this, Path.of("src"), "**/*.java"));
        provider(ResourcesCollector::new)
            .add(newFileTree(this, Path.of("resources"), "**/*",
                ResourceFile.class));
    }

}
