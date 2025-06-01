package jdbld;

import java.nio.file.Path;
import org.jdrupes.builder.api.ResourceFile;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.ResourcesCollector;
import org.jdrupes.builder.java.JavaCompiler;

public class Api extends AbstractProject {

    public Api() {
        name("api");
        generator(JavaCompiler::new)
            .addSources(newFileTree(this, Path.of("src"), "**/*.java"));
        generator(ResourcesCollector::new)
            .add(newFileTree(this, Path.of("resources"), "**/*",
                ResourceFile.class));
    }

}
