package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Eclipse extends AbstractProject implements JavaProject {

    public Eclipse() {
        super(name("eclipse"));
        dependency(Reveal, project(Core.class));
        dependency(Reveal, project(Java.class));
    }
}
