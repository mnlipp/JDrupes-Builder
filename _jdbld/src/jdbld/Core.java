package jdbld;

import static org.jdrupes.builder.api.Intent.*;

import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class Core extends AbstractProject implements JavaProject {

    public Core() {
        super(name("core"));
        dependency(Expose, project(Api.class));
        dependency(Expose, new MvnRepoLookup().resolve(
            "com.google.flogger:flogger:0.9",
            "io.vavr:vavr:0.11.0"));
    }

    public static class CoreTest extends AbstractProject
            implements JavaProject, MergedTestProject {

        public CoreTest() {
            super(parent(Core.class));
            dependency(Consume, project(Core.class));
        }
    }

}
