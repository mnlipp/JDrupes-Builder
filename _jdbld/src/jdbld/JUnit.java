package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class JUnit extends AbstractProject implements JavaProject {

    public JUnit() {
        super(name("junit"));
        dependency(Reveal, project(Core.class));
        dependency(Reveal, project(Java.class));
        dependency(Expose, new MvnRepoLookup()
            .bom("org.junit:junit-bom:5.14.2")
            .resolve("org.junit.platform:junit-platform-launcher"));
    }
}
