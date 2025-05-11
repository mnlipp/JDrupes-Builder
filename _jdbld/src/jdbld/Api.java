package jdbld;

import org.jdrupes.builder.core.FileSet;
import java.nio.file.Path;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.DefaultProject;
import org.jdrupes.builder.java.JavaCompiler;

public class Api extends DefaultProject {

    public Api(Project parent) {
        super(parent, "api");
        provider(new JavaCompiler(this)
            .addSources(new FileSet(this, Path.of("src"), "**/*.java")));
    }

}
