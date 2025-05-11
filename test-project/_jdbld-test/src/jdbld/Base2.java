package jdbld;

import org.jdrupes.builder.core.FileSet;

import java.nio.file.Path;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.DefaultProject;
import org.jdrupes.builder.java.CompileJava;

public class Base2 extends DefaultProject {

    public Base2(Project parent) {
        super(parent, "base2");
        provider(CompileJava::new)
            .addSources(new FileSet(this, Path.of("src"), "**/*.java"));
    }

}
