package jdbld;

import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.AppJarBuilder;
import org.jdrupes.builder.java.JarFile;

public class Root extends AbstractProject implements RootProject {

    public Root() {
        name("jdbuilder");
        provider(AppJarBuilder::new).addAll(subprojects());
    }

    public void provide() {
        provide(new ResourceRequest<>(new ResourceType<JarFile>() {
        })).forEach(System.out::println);
    }
}
