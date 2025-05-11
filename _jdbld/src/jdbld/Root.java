package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.core.DefaultLauncher;
import org.jdrupes.builder.core.DefaultProject;

public class Root extends DefaultProject {

    public Root() {
        super(null, Path.of("."));
        new Java(this);
    }

    public static void main(String[] args) {
        new DefaultLauncher(new Root()).start(args);
    }

}
