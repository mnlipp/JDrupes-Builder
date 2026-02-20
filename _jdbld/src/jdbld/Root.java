package jdbld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Scm;
import org.eclipse.jgit.api.Git;
import static jdbld.ExtProps.GitApi;
import static org.jdrupes.builder.api.Intent.*;
import static org.jdrupes.builder.api.Project.Properties.Version;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.api.TestResult;
import org.jdrupes.builder.core.AbstractRootProject;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.eclipse.EclipseConfigurator;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceCollector;
import org.jdrupes.builder.mvnrepo.JavadocJarBuilder;
import org.jdrupes.builder.mvnrepo.MvnPublication;
import org.jdrupes.builder.mvnrepo.MvnPublisher;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.mvnrepo.PomFile;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.jdrupes.builder.mvnrepo.SourcesJarGenerator;
import org.jdrupes.builder.uberjar.UberJarBuilder;
import org.jdrupes.builder.vscode.VscodeConfiguration;
import org.jdrupes.builder.vscode.VscodeConfigurator;
import org.jdrupes.gitversioning.api.VersionEvaluator;
import org.jdrupes.gitversioning.core.DefaultTagFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static org.jdrupes.builder.mvnrepo.MvnProperties.*;
import org.jdrupes.builder.java.Javadoc;
import org.jdrupes.builder.java.JavadocDirectory;
import org.jdrupes.builder.java.JavadocJarFile;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.SourcesJarFile;
import org.jdrupes.builder.junit.JUnitTestRunner;

public class Root extends AbstractRootProject {

    @Override
    public void prepareProject(Project project) {
        setupVersion(project);
        setupCommonGenerators(project);
        setupEclipseConfigurator(project);
        setupVscodeConfiguration(project);
    }

    public Root() {
        super(name("jdrupes-builder"));
        set(GroupId, "org.jdrupes");

        dependency(Expose, project(Api.class));
        dependency(Expose, project(Core.class));
        dependency(Expose, project(Java.class));
        dependency(Expose, project(Bnd.class));
        dependency(Expose, project(MvnRepo.class));
        dependency(Expose, project(Uberjar.class));
        dependency(Expose, project(Startup.class));
        dependency(Expose, project(Eclipse.class));
        dependency(Expose, project(Vscode.class));
        dependency(Expose, project(JUnit.class));

        // Generate POM
        generator(PomFileGenerator::new).adaptPom(model -> {
            model.setDescription("See URL.");
            model.setUrl("https://builder.jdrupes.org/");
            var scm = new Scm();
            scm.setUrl("https://github.com/jdrupes/jdrupes-builder");
            scm.setConnection(
                "scm:git://github.com/jdrupes/jdrupes-builder.git");
            scm.setDeveloperConnection(
                "scm:git://github.com/jdrupes/jdrupes-builder.git");
            model.setScm(scm);
            var license = new License();
            license.setName("AGPL 3.0");
            license.setUrl("https://www.gnu.org/licenses/agpl-3.0.en.html");
            license.setDistribution("repo");
            model.setLicenses(List.of(license));
            var developer = new Developer();
            developer.setId("mnlipp");
            developer.setName("Michael N. Lipp");
            model.setDevelopers(List.of(developer));
        });

        // Provide app jar
        generator(new UberJarBuilder(this)
            // bndlib and its dependencies are a mess
            .ignoreDuplicates(
                "about.html", "OSGI-OPT/src/**", "LICENSE", "aQute/**")
            .addFrom(providers().select(Expose))
            // Runtime (only) dependencies of executable jar
            .addFrom(new MvnRepoLookup().resolve(
                "com.google.flogger:flogger-system-backend:0.9",
                "eu.maveniverse.maven.mima.runtime:standalone-static:2.4.34",
                "org.slf4j:slf4j-api:2.0.17",
                "org.slf4j:slf4j-jdk14:2.0.17"))
            .mainClass("org.jdrupes.builder.startup.BootstrapProjectLauncher")
            .addEntries(resources(of(PomFile.class).using(Supply))
                .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                    .resolve((String) get(GroupId)).resolve(name())
                    .resolve("pom.xml"), pomFile)))
            .destination(buildDirectory().resolve(Path.of("app"))));

        // Supply javadoc
        generator(Javadoc::new)
            .destination(rootProject().directory().resolve("webpages/javadoc"))
            .tagletpath(new MvnRepoLookup()
                .resolve("org.jdrupes.taglets:plantuml-taglet:3.1.0",
                    "net.sourceforge.plantuml:plantuml:1.2023.11")
                .resources(of(ClasspathElementType).using(Supply, Expose)))
            .taglets(Stream.of("org.jdrupes.taglets.plantUml.PlantUml",
                "org.jdrupes.taglets.plantUml.StartUml",
                "org.jdrupes.taglets.plantUml.EndUml"))
            .options("-overview", directory().resolve("overview.md").toString())
            .options("--add-stylesheet",
                directory().resolve("misc/javadoc-overwrites.css").toString())
            .options("--add-script",
                directory().resolve("misc/highlight.min.js").toString())
            .options("--add-script",
                directory().resolve("misc/highlight-all.js").toString())
            .options("--add-stylesheet",
                directory().resolve("misc/highlight-default.css").toString())
            .options("-bottom",
                readString(directory().resolve("misc/javadoc.bottom.txt")))
            .options("--allow-script-in-comments")
            .options("-linksource")
            .options("-link",
                "https://docs.oracle.com/en/java/javase/25/docs/api/")
            .options("-link",
                "https://maven.apache.org/ref/3-LATEST/apidocs/")
            .options("-quiet");

        // Supply sources jar
        generator(SourcesJarGenerator::new).addTrees(resources(
            of(JavaSourceTreeType).using(Supply, Expose)));

        // Supply javadoc jar
        generator(JavadocJarBuilder::new);

        // Publish (deploy). Credentials and signing information is
        // obtained through properties.
        generator(MvnPublisher::new);

        // Commands
        commandAlias("build").resources(of(AppJarFile.class),
            of(JavadocDirectoryType));
        commandAlias("test").resources(of(TestResult.class));
        commandAlias("sourcesJar").resources(of(SourcesJarFile.class));
        commandAlias("javadoc").resources(of(JavadocDirectory.class));
        commandAlias("javadocJar").resources(of(JavadocJarFile.class));
        commandAlias("eclipse").resources(of(EclipseConfiguration.class));
        commandAlias("vscode").resources(of(VscodeConfiguration.class));
        commandAlias("pomFile").resources(of(PomFile.class));
        commandAlias("mavenPublication").resources(of(MvnPublication.class));
    }

    public static void setupVersion(Project project) {
        try {
            if (project instanceof RootProject) {
                project.set(GitApi, Git.open(project.directory().toFile()));
            }
        } catch (IOException e) {
            throw new BuildException().from(project).cause(e);
        }

        var evaluator = VersionEvaluator
            .forRepository(project.<Git> get(GitApi).getRepository())
            .subDirectory(project.directory())
            .tagFilter(new DefaultTagFilter().prepend("v"));
        project.set(Version, evaluator.version());

    }

    private static void setupCommonGenerators(Project project) {
        if (project instanceof JavaProject) {
            if (!(project instanceof MergedTestProject)) {
                project.generator(JavaCompiler::new)
                    .addSources(Path.of("src"), "**/*.java")
                    .options("--release", "25");
                project.generator(JavaResourceCollector::new)
                    .add(Path.of("resources"), "**/*");
            } else {
                project.generator(JavaCompiler::new).addSources(Path.of("test"),
                    "**/*.java").options("--release", "25");
                project.generator(JavaResourceCollector::new).add(Path.of(
                    "test-resources"), "**/*");
                project.dependency(Consume, new MvnRepoLookup()
                    .resolve("junit:junit:4.13.2")
                    .bom("org.junit:junit-bom:5.14.2")
                    .resolve("org.junit.jupiter:junit-jupiter-api")
                    .resolve("org.junit.jupiter:junit-jupiter-params")
                    .resolve("org.junit.jupiter:junit-jupiter-engine",
                        "org.junit.vintage:junit-vintage-engine",
                        "net.jodah:concurrentunit:0.4.2"));
                project.dependency(Supply, JUnitTestRunner::new);
            }
        }
    }

    private static void setupEclipseConfigurator(Project project) {
        project.generator(new EclipseConfigurator(project)
            .eclipseAlias(project instanceof RootProject ? project.name()
                : project.get(GroupId) + ".builder." + project.name())
            .adaptProjectConfiguration((Document doc,
                    Node buildSpec, Node natures) -> {
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
            }).adaptConfiguration(() -> {
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

    @SuppressWarnings("unchecked")
    private static void setupVscodeConfiguration(Project project) {
        project.generator(VscodeConfigurator::new)
            .jdk("25", Path.of("/usr/lib/jvm/java-25-openjdk/"))
            .adaptTasks(c -> {
                if (!(project instanceof Root)) {
                    return;
                }
                ((List<Map<String, Object>>) c.get("tasks")).addAll(List.of(
                    Map.of("label", "Build JDrupes Builder",
                        "type", "shell",
                        "group", "build",
                        "command",
                        "JDBLD_JAR=build/app/jdrupes-builder-current.jar"
                          + " ./jdbld -B-x \"test-projects/*project*\" build",
                        "problemMatcher", List.of("$gcc")),
                    Map.of("label", "Generate vscode configuration",
                        "type", "shell",
                        "group", "build",
                        "command",
                        "JDBLD_JAR=build/app/jdrupes-builder-current.jar"
                            + " ./jdbld -B-x \"test-projects/*project*\" vscode",
                        "problemMatcher", List.of("$gcc"))));
            })
            .adaptLaunch(c -> {
                if (!(project instanceof Root)) {
                    return;
                }
                ((List<Map<String, Object>>) c.get("configurations"))
                    .addAll(List.of(
                        Map.of("type", "java",
                            "name", "Current File",
                            "request", "launch",
                            "mainClass", "${file}",
                            "vmArgs", "-Duser.language=en_US",
                            "args", "build"),
                        Map.of("type", "java",
                            "name", "BootstrapLauncher",
                            "request", "launch",
                            "classPaths", List.of("$Auto",
                                "${workspaceFolder}/eclipse/build/classes",
                                "${workspaceFolder}/uberjar/build/classes",
                                "${workspaceFolder}/vscode/build/classes",
                                "${workspaceFolder}/build/app/jdrupes-builder-0.0.3-SNAPSHOT.jar"),
                            "mainClass",
                            "org.jdrupes.builder.startup.BootstrapLauncher",
                            "projectName", "startup",
                            "vmArgs", "-Duser.language=en_US",
                            "args", List.of("-B-x", "test-*-project", "build")),
                        Map.of("type", "java",
                            "name", "DirectLauncher",
                            "request", "launch",
                            "classPaths", List.of("$Auto",
                                "${workspaceFolder}/_jdbld/build/classes",
                                "${workspaceFolder}/eclipse/build/classes",
                                "${workspaceFolder}/uberjar/build/classes",
                                "${workspaceFolder}/vscode/build/classes",
                                "${workspaceFolder}/build/app/jdrupes-builder-0.0.3-SNAPSHOT.jar"),
                            "mainClass",
                            "org.jdrupes.builder.startup.DirectLauncher",
                            "projectName", "startup",
                            "vmArgs", "-Duser.language=en_US",
                            "args", "build")));
            });
    }

}
