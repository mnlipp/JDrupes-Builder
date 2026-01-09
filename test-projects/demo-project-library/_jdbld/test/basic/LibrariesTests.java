package basic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.Launcher;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.mvnrepo.PomFile;
import static org.jdrupes.builder.api.ResourceRequest.requestFor;
import static org.junit.jupiter.api.Assertions.*;

import org.jdrupes.builder.startup.DirectLauncher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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

    @Test
    public void testLibraries() throws IOException {
        var jars = launcher.provide(Stream.of(launcher.rootProject()),
            requestFor(JarFile.class)).collect(Collectors.toSet());
        var paths = jars.stream().map(JarFile::path).toList();
        var apiLib = paths.stream().filter(p -> p.toString()
            .endsWith("api/build/libs/api-0.0.0.jar")).findAny();
        assertTrue(apiLib.isPresent());
        try (var jarFile = new java.util.jar.JarFile(apiLib.get().toFile())) {
            var entryNames = Collections.list(jarFile.entries()).stream()
                .map(e -> e.getName()).toList();
            List.of(
                "META-INF/maven/org.jdrupes.builder.demo.library/api/pom.xml")
                .stream().forEach(e -> assertTrue(entryNames.contains(e)));
        }

        var implLib = paths.stream().filter(p -> p.toString()
            .endsWith("impl/build/libs/impl-0.0.0.jar")).findAny();
        assertTrue(implLib.isPresent());
        try (var jarFile = new java.util.jar.JarFile(implLib.get().toFile())) {
            var entryNames = Collections.list(jarFile.entries()).stream()
                .map(e -> e.getName()).toList();
            List.of(
                "META-INF/maven/org.jdrupes.builder.demo.library/impl/pom.xml")
                .stream().forEach(e -> assertTrue(entryNames.contains(e)));
        }
    }

    @Test
    public void testPom()
            throws IOException, ParserConfigurationException, SAXException {
        // Request POMs
        var poms = launcher.provide(Stream.of(launcher.rootProject()),
            requestFor(PomFile.class)).collect(Collectors.toSet());
        var implPom = poms.stream().map(PomFile::path)
            .filter(p -> p.toString().contains("impl-pom.xml")).findAny();
        assertTrue(implPom.isPresent());

        // Read POM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(implPom.get().toFile());
        document.getDocumentElement().normalize();
        Element root = document.getDocumentElement();

        // Get <dependency> elements
        var depsNode
            = (Element) root.getElementsByTagName("dependencies").item(0);
        var depsList = depsNode.getElementsByTagName("dependency");
        var depEls = IntStream.range(0, depsList.getLength())
            .mapToObj(depsList::item).filter(n -> n instanceof Element)
            .map(Element.class::cast).toList();

        // api dependency
        var apiDep = depEls
            .stream().filter(e -> "org.jdrupes.builder.demo.library"
                .equals(e.getElementsByTagName("groupId").item(0)
                    .getTextContent())
                && "api".equals(e.getElementsByTagName("artifactId").item(0)
                    .getTextContent()))
            .findAny();
        assertTrue(apiDep.isPresent());
        assertEquals("compile", apiDep.get().getElementsByTagName("scope")
            .item(0).getTextContent());

        // snake dependency
        var snakeDep = depEls.stream().filter(e -> "org.yaml"
            .equals(e.getElementsByTagName("groupId").item(0).getTextContent())
            && "snakeyaml".equals(e.getElementsByTagName("artifactId").item(0)
                .getTextContent()))
            .findAny();
        assertTrue(snakeDep.isPresent());
        assertEquals("runtime", snakeDep.get().getElementsByTagName("scope")
            .item(0).getTextContent());
    }
}