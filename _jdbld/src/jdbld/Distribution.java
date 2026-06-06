package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Distribution extends AbstractProject implements JavaProject {

    public Distribution() {
        super(name("distribution"));
        dependency(Reveal, project(Core.class));
        dependency(Reveal, project(Java.class));
        dependency(Reveal, project(MvnRepo.class));
    }

}
