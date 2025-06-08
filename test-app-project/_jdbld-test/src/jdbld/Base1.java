package jdbld;

import java.nio.file.Path;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaResourceCollector;

public class Base1 extends AbstractProject {

    public Base1() {
        name("base1");
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        generator(JavaResourceCollector::new).add(Path.of("resources"), "**/*");
    }

}
