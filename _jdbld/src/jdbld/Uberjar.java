package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Uberjar extends AbstractProject implements JavaProject {

    public Uberjar() {
        super(name("uberjar"));
        dependency(Reveal, project(Core.class));
        dependency(Reveal, project(Java.class));
        dependency(Reveal, project(MvnRepo.class));
    }

}
