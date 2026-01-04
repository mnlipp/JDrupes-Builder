package jdbld;

import static org.jdrupes.builder.api.Intend.Consume;
import static org.jdrupes.builder.api.Intend.Expose;
import static org.jdrupes.builder.mvnrepo.MvnProperties.GroupId;

import java.nio.file.Path;
import java.util.Map;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.api.TestResult;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.mvnrepo.PomFile;
import org.jdrupes.builder.mvnrepo.MvnRepoDependency.Scope;
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
            new ResourceRequest<JarFile>(new ResourceType<>() {}));
        commandAlias("test",
            new ResourceRequest<TestResult>(new ResourceType<>() {}));
        commandAlias("eclipse",
            new ResourceRequest<EclipseConfiguration>(new ResourceType<>() {}));
    }

}
