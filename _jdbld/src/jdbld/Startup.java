package jdbld;

import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Startup extends AbstractProject implements JavaProject {

    public Startup() {
        super(name("startup"));
        dependency(project(Core.class), Intend.Consume);
        dependency(project(Java.class), Intend.Consume);
    }

}
