package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.api.TestResult;
import static org.jdrupes.builder.bnd.BndProperties.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.mvnrepo.PomFile;
import static org.jdrupes.builder.mvnrepo.MvnProperties.*;
import java.util.Map;

public class Root extends AbstractProject implements RootProject {

    @Override
    public void prepareProject(Project project) {
        ProjectPreparation.setupCommonGenerators(project);
        ProjectPreparation.setupEclipseConfigurator(project);
    }

    public Root() {
        super(name("demo-project-library"));
        set(GroupId, "org.jdrupes.builder.demo.library");
        set(BndInstructions, Map.of(
            "Bundle-Copyright", "Michael N. Lipp (c) 2026",
            "Bundle-License", "http://www.gnu.org/licenses/agpl-3.0.txt",
            "-diffignore", "Git-Descriptor, Git-SHA"));

        // Commands
        commandAlias("build")
            .resources(of(JarFile.class).using(Forward, Supply, Expose));
        commandAlias("test").resources(of(TestResult.class));
        commandAlias("pomFile").resources(of(PomFile.class));
        commandAlias("eclipse").resources(of(EclipseConfiguration.class));
    }

}
