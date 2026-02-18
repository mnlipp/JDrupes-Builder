package jdbld;

import static org.jdrupes.builder.api.Intent.Consume;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractRootProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.eclipse.EclipseConfigurator;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceCollector;
import org.jdrupes.builder.java.LibraryBuilder;

public class Root extends AbstractRootProject {

    @Override
    public void prepareProject(Project project) {
        setupCommonGenerators(project);
        setupEclipseConfigurator(project);
    }

    public Root() {
        super(name("app-project_1"));
        commandAlias("build").resources(of(JarFile.class));
        commandAlias("eclipse").resources(of(EclipseConfiguration.class));
    }

    private static void setupCommonGenerators(Project project) {
        if (project instanceof JavaProject) {
            project.dependency(Consume, JavaCompiler::new)
                .addSources(Path.of("src"), "**/*.java");
            project.dependency(Consume, JavaResourceCollector::new)
                .add(Path.of("resources"), "**/*");
            project.generator(LibraryBuilder::new)
                .addFrom(project.providers().select(Consume));
        }
    }

    private static void setupEclipseConfigurator(Project project) {
        project.generator(new EclipseConfigurator(project)
            .eclipseAlias(project instanceof RootProject ? project.name()
                : "org.jdrupes.builder.test.app1." + project.name())
            .adaptProjectConfiguration((doc, buildSpec, natures) -> {
                if (project instanceof JavaProject) {
                    var cmd = buildSpec
                        .appendChild(doc.createElement("buildCommand"));
                    cmd.appendChild(doc.createElement("name"))
                        .appendChild(doc.createTextNode(
                            "net.sf.eclipsecs.core.CheckstyleBuilder"));
                    cmd.appendChild(doc.createElement("arguments"));
                    natures.appendChild(doc.createElement("nature"))
                        .appendChild(doc.createTextNode(
                            "net.sf.eclipsecs.core.CheckstyleNature"));
                    cmd = buildSpec
                        .appendChild(doc.createElement("buildCommand"));
                    cmd.appendChild(doc.createElement("name"))
                        .appendChild(doc.createTextNode(
                            "ch.acanda.eclipse.pmd.builder.PMDBuilder"));
                    cmd.appendChild(doc.createElement("arguments"));
                    natures.appendChild(doc.createElement("nature"))
                        .appendChild(doc.createTextNode(
                            "ch.acanda.eclipse.pmd.builder.PMDNature"));
                }
            })
            .adaptConfiguration(() -> {
                if (!(project instanceof JavaProject)) {
                    return;
                }
                try {
                    Files.copy(
                        Root.class.getResourceAsStream("net.sf.jautodoc.prefs"),
                        project.directory()
                            .resolve(".settings/net.sf.jautodoc.prefs"),
                        StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(Root.class.getResourceAsStream("checkstyle"),
                        project.directory().resolve(".checkstyle"),
                        StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(Root.class.getResourceAsStream("eclipse-pmd"),
                        project.directory().resolve(".eclipse-pmd"),
                        StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new BuildException().from(project).cause(e);
                }
            }));
    }
}
