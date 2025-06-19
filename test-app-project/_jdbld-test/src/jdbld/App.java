package jdbld;

import java.nio.file.Path;
import static org.jdrupes.builder.api.Intend.*;

import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.UberJarGenerator;
import org.jdrupes.builder.java.JavaCompiler;

public class App extends AbstractProject {

    public App() {
        super(name("app"));
        dependency(project(Base1.class), Consume);
        dependency(project(Base2.class), Consume);
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        generator(UberJarGenerator::new).add(this);
    }
}
