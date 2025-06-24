package jdbld;

import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Core extends AbstractProject implements JavaProject {

    public Core() {
        super(name("core"));
        dependency(Expose, project(Api.class));
    }

}
