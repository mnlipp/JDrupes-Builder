package jdbld;

import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class JUnit extends AbstractProject implements JavaProject {

    public JUnit() {
        super(name("junit"));
        dependency(Consume, project(Core.class));
        dependency(Consume, project(Java.class));
        dependency(Expose, new MvnRepoLookup().resolve(
            "org.junit.platform:junit-platform-launcher:1.12.2"));
    }
}
