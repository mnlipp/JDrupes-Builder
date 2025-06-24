package jdbld;

import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Eclipse extends AbstractProject implements JavaProject {

    public Eclipse() {
        super(name("eclipse"));
        dependency(Consume, project(Core.class));
        dependency(Consume, project(Java.class));
    }

}
