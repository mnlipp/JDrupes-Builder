package org.jdrupes.builder.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jdrupes.builder.api.AllResources;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.core.AbstractGenerator;

public class AppJarBuilder extends AbstractGenerator<FileResource> {

    private List<ResourceProvider<?>> providers = new ArrayList<>();

    public AppJarBuilder(Project project) {
        super(project);
    }

    public AppJarBuilder add(ResourceProvider<?>... providers) {
        this.providers.addAll(Arrays.asList(providers));
        return this;
    }

    @Override
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.CollapsibleIfStatements" })
    public Stream<FileResource> provide(Resource resource) {
        if (!Resource.KIND_APP_JAR.equals(resource.kind())) {
            return Stream.empty();
        }

        // Get all content.
        log.fine(() -> "Getting app jar content for " + project().name());
        var entries = new LinkedHashMap<Path, Path>();
        for (var provider : providers) {
            addEntries(entries, project().build().provide(provider,
                AllResources.of(Resource.KIND_CLASSES)));
            addEntries(entries, project().build().provide(provider,
                AllResources.of(Resource.KIND_RESOURCE)));
        }

        // Prepare jar file
        log.info(() -> "Building application jar in " + project().name());
        var destDir = project().buildDirectory().resolve("app");
        if (!destDir.toFile().exists()) {
            if (!destDir.toFile().mkdirs()) {
                throw new BuildException("Cannot create directory " + destDir);
            }
        }
        var jarPath = destDir.resolve(project().name() + ".jar");

        // Add content to jar
        Manifest manifest = new Manifest();
        @SuppressWarnings("PMD.LooseCoupling")
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jos
            = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            for (var entry : entries.entrySet()) {
                var path = entry.getValue().resolve(entry.getKey());
                var entryName
                    = StreamSupport.stream(entry.getKey().spliterator(), false)
                        .map(Path::toString).collect(Collectors.joining("/"));
                @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                JarEntry jarEntry = new JarEntry(entryName);
                jarEntry.setTime(path.toFile().lastModified());
                jos.putNextEntry(jarEntry);
                try (var input = Files
                    .newInputStream(entry.getValue().resolve(entry.getKey()))) {
                    input.transferTo(jos);
                }
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }

        // The result is the jar.
        return Stream.of(project().newFileResource(jarPath));
    }

    private void addEntries(Map<Path, Path> entries,
            Stream<? extends Resource> fileSets) {
        fileSets.filter(fs -> fs instanceof FileTree).map(fs -> (FileTree) fs)
            .forEach(fs -> {
                fs.stream().forEach(file -> {
                    var relPath = fs.root().relativize(file.path());
                    var existing = entries.get(relPath);
                    if (existing != null && !existing.equals(fs.root())) {
                        log.warning(() -> "Entry " + relPath
                            + " from file set with root "
                            + project().rootProject().relativize(fs.root())
                            + " duplicates entry from "
                            + project().rootProject().relativize(existing)
                            + " and is skipped.");
                    } else {
                        entries.put(relPath, fs.root());
                    }
                });
            });
    }

}
