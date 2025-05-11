package jdbld;

import org.jdrupes.builder.core.FileSet;

import java.nio.file.Path;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.DefaultProject;
import org.jdrupes.builder.java.JavaCompiler;

public class Java extends DefaultProject {

    public Java(Project parent) {
        super(parent, "java");
        new Core(this);
        provider(new JavaCompiler(this)
            .addSources(new FileSet(this, Path.of("src"), "**/*.java")));
    }

}
