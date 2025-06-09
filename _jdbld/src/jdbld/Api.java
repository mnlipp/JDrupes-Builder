package jdbld;

import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Api extends AbstractProject implements JavaProject {

    public Api() {
        super(name("api"));
    }

}
