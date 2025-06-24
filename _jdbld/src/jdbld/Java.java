package jdbld;

import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Java extends AbstractProject implements JavaProject {

    public Java() {
        super(name("java"));
        dependency(Consume, project(Core.class));
    }

}
