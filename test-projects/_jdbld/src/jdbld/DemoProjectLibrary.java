/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package jdbld;

import java.nio.file.Path;
import static org.jdrupes.builder.api.Intend.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaResourceCollector;
import org.jdrupes.builder.junit.JUnitTestRunner;

public class DemoProjectLibrary extends AbstractProject
        implements JdbldTestProject {

    public DemoProjectLibrary() {
        super(name("demo-project-library"), parent(TestProjects.class));

        dependency(Consume, project(Core.class));
        dependency(Consume, project(Java.class));
        dependency(Consume, project(Uberjar.class));
        dependency(Consume, project(Startup.class));
        dependency(Consume, project(Eclipse.class));
        dependency(Consume, project(JUnit.class));
        dependency(Consume, project(MvnRepo.class));
        TestProjects.prepareProject(this);

        // Consume only generators
        dependency(Consume, JavaCompiler::new).addSources(Path.of("_jdbld/src"),
            "**/*.java").addSources(Path.of("_jdbld/test"), "**/*.java");
        dependency(Consume, JavaResourceCollector::new).add(Path.of(
            "_jdbld/resources"), "**/*");
        dependency(Supply, JUnitTestRunner::new);
    }
}
