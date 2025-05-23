package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.api.Dependency;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.DefaultLauncher;
import org.jdrupes.builder.core.AbstractProject;

public class Root extends AbstractProject {

    public Root(Class<Project>[] subprojects) {
        super(subprojects);
        directory(Path.of("."));
        dependency(project(App.class), Dependency.Intend.Build);
    }

    public static void main(String[] args) {
        new DefaultLauncher(Root.class, App.class, Base1.class, Base2.class)
            .start(args);
    }

}
