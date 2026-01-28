package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class Bnd extends AbstractProject implements JavaProject {

    public Bnd() {
        super(name("bnd"));
        dependency(Expose, project(Core.class));
        dependency(Reveal, project(Java.class));
        dependency(Reveal, new MvnRepoLookup()
            .resolve("biz.aQute.bnd:biz.aQute.bndlib:7.2.1"));
    }
}
