package jdbld;

import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Startup extends AbstractProject implements JavaProject {

    public Startup() {
        super(name("startup"));
        dependency(Consume, project(Core.class));
        dependency(Consume, project(Java.class));
    }

}
