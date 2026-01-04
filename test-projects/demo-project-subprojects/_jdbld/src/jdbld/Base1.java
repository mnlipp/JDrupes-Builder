package jdbld;

import static org.jdrupes.builder.api.Intend.Consume;

import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;

public class Base1 extends AbstractProject implements JavaProject {

    public Base1() {
        super(name("base1"));
    }

    public static class Base1Test extends AbstractProject
            implements JavaProject, MergedTestProject {

        public Base1Test() {
            super(parent(Base1.class));
            dependency(Consume, project(Base1.class));
        }
    }

}
