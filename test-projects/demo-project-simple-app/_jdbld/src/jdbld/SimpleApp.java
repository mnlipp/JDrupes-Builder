package jdbld;

import static org.jdrupes.builder.api.Intent.*;

import java.nio.file.Path;

import org.jdrupes.builder.api.ExecResult;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaExecutor;
import org.jdrupes.builder.uberjar.UberJarGenerator;

public class SimpleApp extends AbstractProject implements RootProject {

    public SimpleApp() {
        super(name("demo-project-simple-app"));
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        var jarGen = generator(UberJarGenerator::new).from(providers()
            .select(Supply)).mainClass("jdbld.demo.simpleapp.App");
        dependency(Supply, JavaExecutor::new).from(jarGen);

        // Command arguments
        commandAlias("build").resources(of(AppJarFile.class));
        commandAlias("run").resources(of(ExecResult.class));
        commandAlias("eclipse").resources(of(EclipseConfiguration.class));
    }
}
