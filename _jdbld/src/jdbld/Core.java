package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.ResourcesCollector;
import org.jdrupes.builder.java.JavaCompiler;

public class Core extends AbstractProject {

    public Core() {
        name("core");
        dependency(project(Api.class), Intend.Expose);
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        generator(ResourcesCollector::new).add(Path.of("resources"), "**/*");
    }

}
