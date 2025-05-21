package jdbld;

import java.nio.file.Path;
import static org.jdrupes.builder.api.Dependency.Intend.Consume;

import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.DefaultProject;
import org.jdrupes.builder.java.AppJarBuilder;
import org.jdrupes.builder.java.JavaCompiler;

public class App extends DefaultProject {

    public App(Project parent) {
        super(parent, "app");
        dependency(Base1::new, Consume);
        dependency(Base2::new, Consume);
        provider(JavaCompiler::new)
            .addSources(newFileTree(this, Path.of("src"), "**/*.java"));
        provider(AppJarBuilder::new);
    }
}
