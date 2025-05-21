package jdbld;

import org.jdrupes.builder.core.FileSet;

import java.nio.file.Path;

import org.jdrupes.builder.api.Dependency;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.DefaultProject;
import org.jdrupes.builder.java.AppJarBuilder;
import org.jdrupes.builder.java.JavaCompiler;

public class App extends DefaultProject {

    public App(Project parent) {
        super(parent, "app");
        dependency(Base1::new, Dependency.Type.Consume);
        dependency(Base2::new, Dependency.Type.Consume);
        provider(JavaCompiler::new)
            .addSources(new FileSet(this, Path.of("src"), "**/*.java"));
        provider(AppJarBuilder::new);
    }
}
