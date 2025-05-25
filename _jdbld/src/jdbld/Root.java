package jdbld;

import java.nio.file.Path;

import org.jdrupes.builder.core.DefaultLauncher;
import org.jdrupes.builder.core.AbstractProject;

public class Root extends AbstractProject {

    public Root() {
        super(Api.class, Core.class, Java.class);
        directory(Path.of("."));
    }

    public static void main(String[] args) {
        new DefaultLauncher(Root.class).start(args);
    }

}
