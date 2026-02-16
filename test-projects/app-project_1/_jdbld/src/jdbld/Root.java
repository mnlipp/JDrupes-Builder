package jdbld;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.AbstractRootProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.java.JarFile;

public class Root extends AbstractRootProject {

    @Override
    public void prepareProject(Project project) {
        ProjectPreparation.setupCommonGenerators(project);
        ProjectPreparation.setupEclipseConfigurator(project);
    }

    public Root() {
        super(name("app-project_1"));
        commandAlias("build").resources(of(JarFile.class));
        commandAlias("eclipse").resources(of(EclipseConfiguration.class));
    }

}
