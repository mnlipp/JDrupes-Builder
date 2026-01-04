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
import org.jdrupes.builder.java.JarFile;
import static org.jdrupes.builder.api.ResourceRequest.requestFor;
import org.jdrupes.builder.startup.DirectLauncher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BuildTest {
    private static Launcher launcher;

    @BeforeAll
    public static void initProject() throws URISyntaxException {
        var buildRoot = Path.of(BuildTest.class.getProtectionDomain()
            .getCodeSource().getLocation().toURI()).getParent().getParent();
        launcher = new DirectLauncher(
            Thread.currentThread().getContextClassLoader(), buildRoot,
            new String[0]);
        launcher.provide(requestFor(Cleanliness.class));
    }

    @Test
    public void testLibraries() throws IOException {
        var jars = launcher.provide(requestFor(JarFile.class))
            .collect(Collectors.toSet());
        var paths = jars.stream().map(JarFile::path).toList();
        assertEquals(1, paths.stream().filter(
            p -> p.toString().endsWith("base1/build/libs/base1-0.0.0.jar"))
            .count());
        assertEquals(1, paths.stream().filter(
            p -> p.toString().endsWith("base2/build/libs/base2-0.0.0.jar"))
            .count());
        assertEquals(1, paths.stream().filter(
            p -> p.toString().endsWith("app/build/libs/app-0.0.0.jar"))
            .count());
        assertEquals(1, paths.stream().filter(
            p -> p.toString().endsWith("app/build/app/app-0.0.0.jar"))
            .count());
        var appJarPath = paths.stream()
            .filter(p -> p.toString().endsWith("app/build/app/app-0.0.0.jar"))
            .findFirst().get();
        try (var jarFile = new java.util.jar.JarFile(appJarPath.toFile())) {
            var entryNames = Collections.list(jarFile.entries()).stream()
                .map(e -> e.getName()).toList();
            List.of("META-INF/MANIFEST.MF", "builder/test/base1/Base1.class",
                "builder/test/base1/test.properties",
                "builder/test/app/App.class", "builder/test/base2/Base2.class")
                .stream().forEach(e -> assertTrue(entryNames.contains(e)));
        }
    }
}