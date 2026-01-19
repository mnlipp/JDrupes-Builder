package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Java extends AbstractProject implements JavaProject {

    public Java() {
        super(name("java"));
        dependency(Reveal, project(Core.class));
    }

}
