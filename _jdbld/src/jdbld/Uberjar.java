package jdbld;

import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Uberjar extends AbstractProject implements JavaProject {

    public Uberjar() {
        super(name("uberjar"));
        dependency(Consume, project(Core.class));
        dependency(Consume, project(Java.class));
        dependency(Consume, project(MvnRepo.class));
    }

}
