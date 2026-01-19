package jdbld;

import static org.jdrupes.builder.api.Intent.*;

import java.nio.file.Path;

import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.uberjar.UberJarGenerator;

public class SimpleApp extends AbstractProject implements RootProject {

    public SimpleApp() {
        super(name("demo-project-simple-app"));
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        generator(UberJarGenerator::new).from(providers().select(Supply))
            .mainClass("jdbld.demo.simpleapp.App");

        // Command arguments
        commandAlias("build", of(AppJarFile.class).using(Supply, Expose));
    }
}
