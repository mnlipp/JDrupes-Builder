package jdbld;

import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Java extends AbstractProject implements JavaProject {

    public Java() {
        name("java");
        dependency(project(Core.class), Intend.Consume);
    }

}
