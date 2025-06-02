package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.ResourcesCollector;
import org.jdrupes.builder.java.JavaCompiler;

public class Java extends AbstractProject {

    public Java() {
        name("java");
        dependency(project(Core.class), Intend.Consume);
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        generator(ResourcesCollector::new).add(Path.of("resources"), "**/*");
    }

}
