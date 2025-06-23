package jdbld;

import java.util.EnumSet;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.UberJarGenerator;

public class App extends AbstractProject implements JavaProject {

    public App() {
        super(name("app"));
        dependency(project(Base1.class), Consume);
        dependency(project(Base2.class), Consume);
        dependency(new UberJarGenerator(this)
            .addAll(providers(EnumSet.of(Consume, Supply))), Intend.Forward);
    }
}
