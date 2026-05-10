package jdbld;

import static org.jdrupes.builder.api.Intent.*;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.FileTreeBuilder;
import org.jdrupes.builder.core.ScriptExecutor;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceTree;

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
        dependency(Consume, ScriptExecutor::new).name("exec1")
            .script("""
                    mkdir -p `dirname $1`
                    echo test = it > $1
                    """)
            .args(buildDirectory().resolve(
                "generated/fromScript/more.properties").toString())
            .provideResources(of(JavaResourceTreeType),
                p -> Stream.of(JavaResourceTree.of(p,
                    p.buildDirectory().resolve("generated/fromScript"), "**")));
    }
}
