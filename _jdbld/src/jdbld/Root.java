package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.api.Dependency.Intend;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.AppJarBuilder;

public class Root extends AbstractProject implements RootProject {

    public Root() {
        directory(Path.of("."));
        name("jdbuilder");
        dependency(project(Api.class), Intend.Expose);
        dependency(project(Core.class), Intend.Expose);
        dependency(project(Java.class), Intend.Expose);
        provider(AppJarBuilder::new);
    }
}
