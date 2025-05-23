package jdbld;

import java.nio.file.Path;
import static org.jdrupes.builder.api.Dependency.Intend.Consume;

import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.AppJarBuilder;
import org.jdrupes.builder.java.JavaCompiler;

public class App extends AbstractProject {

    public App() {
        name("app");
        dependency(project(Base1.class), Consume);
        dependency(project(Base2.class), Consume);
        provider(JavaCompiler::new)
            .addSources(newFileTree(this, Path.of("src"), "**/*.java"));
        provider(AppJarBuilder::new);
    }
}
