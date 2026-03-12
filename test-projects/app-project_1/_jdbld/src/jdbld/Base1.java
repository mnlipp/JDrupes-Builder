package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import java.nio.file.Path;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.FileTreeBuilder;
import org.jdrupes.builder.java.JavaProject;
import static org.jdrupes.builder.java.JavaTypes.*;

public class Base1 extends AbstractProject implements JavaProject {

    public Base1() {
        super(name("base1"));
        dependency(Consume, FileTreeBuilder::new)
            .into(buildDirectory().resolve("generated/copied"))
            .source(Path.of("resources"), "**/*", p -> Path.of(
                p.toString().replace(".properties", "-copy.properties")),
                null)
            .provideResources(of(JavaResourceTreeType));
    }

}
