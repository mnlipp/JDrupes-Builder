package jdbld;

import static org.jdrupes.builder.api.Intent.Expose;

import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class Api extends AbstractProject implements JavaProject {

    public Api() {
        super(name("api"));
        dependency(Expose, new MvnRepoLookup()
            .resolve("commons-cli:commons-cli:1.10.0",
                "io.vavr:vavr:0.11.0"));
    }

}
