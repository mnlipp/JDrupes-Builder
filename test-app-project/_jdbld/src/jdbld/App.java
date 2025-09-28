package jdbld;

import java.util.EnumSet;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.uberjar.UberJarGenerator;

public class App extends AbstractProject implements JavaProject {

    public App() {
        super(name("app"));
        dependency(Consume, project(Base1.class));
        dependency(Consume, project(Base2.class));
        dependency(Forward, new UberJarGenerator(this)
            .addAll(providers(EnumSet.of(Consume, Supply)))
            .destination(buildDirectory().resolve("app")));
    }
}
