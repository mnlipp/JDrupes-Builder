package jdbld;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.api.TestResult;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.mvnrepo.PomFile;

import static org.jdrupes.builder.mvnrepo.MvnProperties.*;

public class Root extends AbstractProject implements RootProject {

    @Override
    public void prepareProject(Project project) {
        ProjectPreparation.setupCommonGenerators(project);
        ProjectPreparation.setupEclipseConfigurator(project);
    }

    public Root() {
        super(name("demo-project-library"));
        set(GroupId, "org.jdrupes.builder.demo.library");

        // Commands
        commandAlias("build",
            this.<JarFile> requestFor(new ResourceType<>() {}));
        commandAlias("test",
            this.<TestResult> requestFor(new ResourceType<>() {}));
        commandAlias("pomFile", requestFor(PomFile.class));
        commandAlias("eclipse",
            this.<EclipseConfiguration> requestFor(new ResourceType<>() {}));
    }

}
