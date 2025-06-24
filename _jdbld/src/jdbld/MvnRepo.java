package jdbld;

import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class MvnRepo extends AbstractProject implements JavaProject {

    public MvnRepo() {
        super(name("mvnrepo"));
        dependency(project(Core.class), Intend.Consume);
        dependency(project(Java.class), Intend.Consume);
        dependency(new MvnRepoLookup().artifact(
            "eu.maveniverse.maven.mima:context:2.4.29"), Intend.Expose);
    }

}
