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

import static org.jdrupes.builder.api.Intent.*;

import java.nio.file.Path;

import org.jdrupes.builder.api.ExecResult;
import org.jdrupes.builder.core.AbstractRootProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaExecutor;
import org.jdrupes.builder.uberjar.UberJarBuilder;

public class SimpleApp extends AbstractRootProject {

    public SimpleApp() {
        super(name("demo-project-simple-app"));
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        generator(UberJarBuilder::new).addFrom(providers()
            .select(Supply)).mainClass("jdbld.demo.simpleapp.App");
        dependency(Supply, JavaExecutor::new)
            .addFrom(providers().filter(p -> p instanceof UberJarBuilder)
                .select(Supply));

        // Command arguments
        commandAlias("build").resources(of(AppJarFile.class));
        commandAlias("run").resources(of(ExecResult.class));
        commandAlias("eclipse").resources(of(EclipseConfiguration.class));
    }
}
