package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class MvnRepo extends AbstractProject implements JavaProject {

    public MvnRepo() {
        super(name("mvnrepo"));
        dependency(Reveal, project(Core.class));
        dependency(Reveal, project(Java.class));
        dependency(Expose, new MvnRepoLookup().resolve(
            "eu.maveniverse.maven.mima:context:2.4.34",
            "org.apache.maven:maven-model-builder:3.9.11",
            "org.bouncycastle:bcpg-jdk18on:1.82"));
    }
}
