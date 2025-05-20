package jdbld;

import org.jdrupes.builder.core.FileSet;
import org.jdrupes.builder.core.ResourceCollector;

import java.nio.file.Path;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.DefaultProject;
import org.jdrupes.builder.java.JavaCompiler;

public class Base1 extends DefaultProject {

    public Base1(Project parent) {
        super(parent, "base1");
        provider(JavaCompiler::new)
            .addSources(new FileSet(this, Path.of("src"), "**/*.java"));
        provider(ResourceCollector::new)
            .add(new FileSet(this, Path.of("resources"), "**/*"));
    }

}
