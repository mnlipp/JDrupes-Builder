package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.core.DefaultLauncher;
import org.jdrupes.builder.core.AbstractProject;

public class Root extends AbstractProject {

    public Root(Class<Project>[] subprojects) {
        super(subprojects);
        directory(Path.of("."));
    }

    public static void main(String[] args) {
        new DefaultLauncher(Root.class, Api.class, Core.class, Java.class)
            .start(args);
    }

}
