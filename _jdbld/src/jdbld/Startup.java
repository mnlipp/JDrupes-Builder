package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Startup extends AbstractProject implements JavaProject {

    public Startup() {
        super(name("startup"));
        dependency(Reveal, project(Core.class));
        dependency(Reveal, project(Java.class));
        dependency(Reveal, project(MvnRepo.class));
    }

}
