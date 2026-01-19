package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.uberjar.UberJarGenerator;

public class App extends AbstractProject implements JavaProject {

    public App() {
        super(name("app"));
        dependency(Expose, project(Base1.class));
        dependency(Expose, project(Base2.class));
        dependency(Forward, new UberJarGenerator(this)
            .from(providers().select(Expose, Supply))
            .destination(buildDirectory().resolve("app")));
    }
}
