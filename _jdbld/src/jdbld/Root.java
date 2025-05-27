package jdbld;

import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.AppJarBuilder;

public class Root extends AbstractProject implements RootProject {

    public Root() {
        name("jdbuilder");

        // Java references "core" and "api".
        provider(AppJarBuilder::new).add(project(Java.class));
    }
}
