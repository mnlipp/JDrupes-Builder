package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class NodeJs extends AbstractProject implements JavaProject {

    public NodeJs() {
        super(name("nodejs"));
        dependency(Reveal, project(Core.class));
    }

}
