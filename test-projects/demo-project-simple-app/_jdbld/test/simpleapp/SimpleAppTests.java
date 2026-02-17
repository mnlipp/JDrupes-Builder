package simpleapp;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.ExecResult;

import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.startup.BuildProjectLauncher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SimpleAppTests {
    private static Launcher launcher;

    @BeforeAll
    public static void initProject() throws URISyntaxException {
        var buildRoot = Path.of(SimpleAppTests.class.getProtectionDomain()
            .getCodeSource().getLocation().toURI())
            .getParent().getParent().getParent();
        launcher = new BuildProjectLauncher(
            Thread.currentThread().getContextClassLoader(), buildRoot,
            new String[0]);
        launcher.resources(launcher.rootProject().projects("**"),
            launcher.rootProject().of(Cleanliness.class).usingAll());
    }

    @AfterAll
    public static void cleanUp() throws Exception {
        launcher.close();
    }

    @Test
    public void testAppJar() throws IOException {
        var jars = launcher.resources(Stream.of(launcher.rootProject()),
            launcher.rootProject().of(AppJarFile.class).using(Supply, Expose))
            .collect(Collectors.toSet());
        var paths = jars.stream().map(JarFile::path).toList();
        assertEquals(1, paths.stream().filter(p -> p.toString()
            .endsWith("build/libs/demo-project-simple-app-0.0.0.jar"))
            .count());
        var appJarPath = paths.stream().filter(
            p -> p.toString().endsWith("demo-project-simple-app-0.0.0.jar"))
            .findFirst().get();
        try (var jarFile = new java.util.jar.JarFile(appJarPath.toFile())) {
            var entryNames = Collections.list(jarFile.entries()).stream()
                .map(e -> e.getName()).toList();
            List.of("META-INF/MANIFEST.MF", "jdbld/demo/simpleapp/App.class")
                .stream().forEach(e -> assertTrue(entryNames.contains(e)));
            assertEquals("jdbld.demo.simpleapp.App", jarFile.getManifest()
                .getMainAttributes().get(Attributes.Name.MAIN_CLASS));
        }
    }

    @Test
    public void testExec() {
        var execResults = launcher.resources(Stream.of(launcher.rootProject()),
            launcher.rootProject().of(ExecResult.class).usingAll()).toList();
        assertEquals(1, execResults.size());
        var execResult = execResults.get(0);
        assertEquals(0, execResult.exitValue());
    }
}