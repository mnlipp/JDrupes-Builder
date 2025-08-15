package jdbld;

import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class Startup extends AbstractProject implements JavaProject {

    public Startup() {
        super(name("startup"));
        dependency(Consume, project(Core.class));
        dependency(Consume, project(Java.class));
        dependency(Consume, new MvnRepoLookup().artifact(
            "eu.maveniverse.maven.mima:context:2.4.29"));
    }

}
