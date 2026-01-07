package basic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.java.JarFile;
import static org.jdrupes.builder.api.ResourceRequest.requestFor;
import org.jdrupes.builder.startup.DirectLauncher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SubprojectsTests {
    private static Launcher launcher;

    @BeforeAll
    public static void initProject() throws URISyntaxException {
        var buildRoot = Path.of(SubprojectsTests.class.getProtectionDomain()
            .getCodeSource().getLocation().toURI()).getParent().getParent();
        launcher = new DirectLauncher(
            Thread.currentThread().getContextClassLoader(), buildRoot,
            new String[0]);
        launcher.provide(requestFor(Cleanliness.class));
    }

    @Test
    public void testTraversalAll() {
        var prjs = launcher.rootProject().projects("**")
            .map(Project::name).toList();
        List.of("demo-project-subprojects", "app", "base1", "base2",
            "Base1Test").stream().forEach(
                e -> assertTrue(prjs.contains(e), () -> e + " is missing"));
    }

    @Test
    public void testTraversalRoot() {
        var prjs = launcher.rootProject().projects("")
            .map(Project::name).toList();
        List.of("demo-project-subprojects").stream().forEach(
            e -> assertTrue(prjs.contains(e), () -> e + " is missing"));
    }

    @Test
    public void testLibraries() throws IOException {
        var jars = launcher.provide(requestFor(JarFile.class))
            .collect(Collectors.toSet());
        var paths = jars.stream().map(JarFile::path).toList();
        assertEquals(1, paths.stream().filter(
            p -> p.toString()
                .endsWith("build/app/demo-project-subprojects-0.0.0.jar"))
            .count());
        var appJarPath = paths.stream().filter(p -> p.toString()
            .endsWith("build/app/demo-project-subprojects-0.0.0.jar"))
            .findFirst().get();
        try (var jarFile = new java.util.jar.JarFile(appJarPath.toFile())) {
            var entryNames = Collections.list(jarFile.entries()).stream()
                .map(e -> e.getName()).toList();
            List.of("META-INF/MANIFEST.MF",
                "jdbld/demo/subprojects/base1/Base1.class",
                "jdbld/demo/subprojects/base2/Base2.class",
                "jdbld/demo/subprojects/app/App.class")
                .stream().forEach(e -> assertTrue(entryNames.contains(e)));
        }
    }
}