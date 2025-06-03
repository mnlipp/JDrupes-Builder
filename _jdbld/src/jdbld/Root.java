package jdbld;

import static org.jdrupes.builder.api.Intend.*;

import java.util.stream.Collectors;

import org.jdrupes.builder.api.BuildContext;
import org.jdrupes.builder.api.Intend;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.AppJarBuilder;
import org.jdrupes.builder.java.JavaConsts;
import org.jdrupes.builder.java.JavaDoc;

public class Root extends AbstractProject implements RootProject {

    @Override
    public void setupDefaults(BuildContext buildContext) {
        int i = 0;
    }

    public Root() {
        name("jdbuilder");

        dependency(project(Api.class), Expose);
        dependency(project(Core.class), Expose);
        dependency(project(Java.class), Expose);
        dependency(project(Startup.class), Expose);

        // Build app jar
        generator(AppJarBuilder::new).addAll(providers(Intend.CONTRIBUTORS));

        // Build javadoc
        generator(JavaDoc::new).addSources(get(this,
            new ResourceRequest<>(JavaConsts.JAVA_SOURCE_FILES)));
    }

    public void provide() {
        provide(new ResourceRequest<>(JavaConsts.JAR_FILE))
            .forEach(System.out::println);
        provide(new ResourceRequest<>(JavaConsts.JAVADOC_DIRECTORY))
            .collect(Collectors.toSet()).stream().forEach(System.out::println);
        ;
    }
}
