package org.jdrupes.builder.startup;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jdrupes.builder.api.AllResources;
import org.jdrupes.builder.api.Dependency.Intend;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.DefaultLauncher;
import org.jdrupes.builder.core.ResourcesCollector;
import org.jdrupes.builder.java.ClasspathProvider;
import org.jdrupes.builder.java.JavaCompiler;

public class BootstrapProject extends AbstractProject {

    public BootstrapProject() {
        // TODO: support for starting from jar
        directory(Path.of(".jdbld"));
        dependency(
            new ClasspathProvider(this, System.getProperty("java.class.path")),
            Intend.Consume);
        // TODO: configurable pattern
        var bldrDirs = newFileTree(this, Path.of("").toAbsolutePath(),
            "**/_jdbld", true);
        var srcTrees = bldrDirs.stream()
            .map(r -> newFileTree(this, r.path().resolve("src"), "**/*.java"))
            .toArray(FileTree[]::new);
        provider(JavaCompiler::new).addSources(srcTrees);
        var resourceTrees = bldrDirs.stream()
            .map(r -> newFileTree(this, r.path().resolve("resources"), "**/*"))
            .toArray(FileTree[]::new);
        provider(ResourcesCollector::new).add(resourceTrees);
    }

}
