package jdbld;

import static org.jdrupes.builder.api.Intend.*;

import java.nio.file.Path;

import static org.jdrupes.builder.api.ResourceRequest.requestFor;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.uberjar.UberJarGenerator;

public class SimpleApp extends AbstractProject implements RootProject {

    public SimpleApp() {
        super(name("demo-project-simple-app"));
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        generator(UberJarGenerator::new).from(providers(Supply))
            .mainClass("jdbld.demo.simpleapp.App");

        // Command arguments
        commandAlias("build", requestFor(AppJarFile.class));
    }
}
