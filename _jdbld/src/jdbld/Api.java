package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.ResourceCollector;
import org.jdrupes.builder.java.JavaCompiler;

public class Api extends AbstractProject {

    public Api() {
        name("api");
        provider(JavaCompiler::new)
            .addSources(newFileTree(this, Path.of("src"), "**/*.java"));
        provider(ResourceCollector::new)
            .add(newFileTree(this, Path.of("resources"), "**/*"));
    }

}
