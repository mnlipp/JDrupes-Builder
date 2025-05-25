package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.core.DefaultLauncher;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;

public class Root extends AbstractProject implements RootProject {

    public Root() {
        directory(Path.of("."));
    }

    public static void main(String[] args) {
        new DefaultLauncher().start(args);
    }

}
