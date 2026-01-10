package jdbld;

import static org.jdrupes.builder.api.Intend.*;
import java.nio.file.Path;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.api.TestResult;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.uberjar.UberJarGenerator;

public class Root extends AbstractProject implements RootProject {

    @Override
    public void prepareProject(Project project) {
        ProjectPreparation.setupCommonGenerators(project);
        ProjectPreparation.setupEclipseConfigurator(project);
    }

    public Root() {
        super(name("demo-project-subprojects"));

        // Explicitly register subprojects
        dependency(Expose, project(App.class));
        dependency(Expose, project(Base1.class));
        dependency(Expose, project(Base2.class));

        // Provide app jar
        generator(new UberJarGenerator(this)
            .from(providers(Expose))
            .mainClass("jdbld.demo.subprojects.app")
            .destination(buildDirectory().resolve(Path.of("app"))));

        commandAlias("build",
            this.<JarFile> requestFor(new ResourceType<>() {}));
        commandAlias("test",
            this.<TestResult> requestFor(new ResourceType<>() {}));
        commandAlias("eclipse",
            this.<EclipseConfiguration> requestFor(new ResourceType<>() {}));
    }

}
