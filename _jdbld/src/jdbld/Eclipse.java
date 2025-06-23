package jdbld;

import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Eclipse extends AbstractProject implements JavaProject {

    public Eclipse() {
        super(name("eclipse"));
        dependency(project(Core.class), Intend.Consume);
        dependency(project(Java.class), Intend.Consume);
    }

}
