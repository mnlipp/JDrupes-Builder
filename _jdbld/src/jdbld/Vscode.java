package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class Vscode extends AbstractProject implements JavaProject {

    public Vscode() {
        super(name("vscode"));
        dependency(Reveal, project(Core.class));
        dependency(Reveal, project(Java.class))
            .dependency(Expose, new MvnRepoLookup().resolve(
                "com.fasterxml.jackson.core:jackson-databind:2.17.0"));
    }

}
