package basic;

import java.net.URISyntaxException;
import java.nio.file.Path;
import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.Launcher;
import static org.jdrupes.builder.api.ResourceRequest.requestFor;
import org.jdrupes.builder.startup.DirectLauncher;
import org.junit.jupiter.api.BeforeAll;

class LibrariesTests {
    private static Launcher launcher;

    @BeforeAll
    public static void initProject() throws URISyntaxException {
        var buildRoot = Path.of(LibrariesTests.class.getProtectionDomain()
            .getCodeSource().getLocation().toURI()).getParent().getParent();
        launcher = new DirectLauncher(
            Thread.currentThread().getContextClassLoader(), buildRoot,
            new String[0]);
        launcher.provide(launcher.rootProject().projects("**"),
            requestFor(Cleanliness.class));
    }

}