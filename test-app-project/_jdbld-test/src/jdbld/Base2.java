package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaCompiler;

public class Base2 extends AbstractProject {

    public Base2() {
        name("base2");
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
    }

}
