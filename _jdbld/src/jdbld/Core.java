package jdbld;

import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Core extends AbstractProject implements JavaProject {

    public Core() {
        name("core");
        dependency(project(Api.class), Intend.Expose);
    }

}
