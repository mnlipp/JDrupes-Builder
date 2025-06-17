package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaResourceCollector;

public class Startup extends AbstractProject {

    public Startup() {
        super(name("startup"));
        dependency(project(Core.class), Intend.Consume);
        dependency(project(Java.class), Intend.Consume);
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        generator(JavaResourceCollector::new).add(Path.of("resources"),
            "**/*");
    }

}
