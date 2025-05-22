package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.DefaultProject;
import org.jdrupes.builder.java.JavaCompiler;

public class Api extends DefaultProject {

    public Api(Project parent) {
        super(parent, "api");
        provider(JavaCompiler::new)
            .addSources(newFileTree(this, Path.of("src"), "**/*.java"));
    }

}
