package jdbld;

import org.jdrupes.builder.core.ResourcesCollector;

import java.nio.file.Path;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaCompiler;

public class Base1 extends AbstractProject {

    public Base1() {
        name("base1");
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        generator(ResourcesCollector::new).add(Path.of("resources"), "**/*");
    }

}
