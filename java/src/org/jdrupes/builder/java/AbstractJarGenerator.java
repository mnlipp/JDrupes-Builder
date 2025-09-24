/*
 * JDrupes Builder
 * Copyright (C) 2025 Michael N. Lipp
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

package org.jdrupes.builder.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.IOResource;
import org.jdrupes.builder.api.Project;
import static org.jdrupes.builder.api.Project.Properties.*;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.core.CachedStream;

/// A base class for generators tha generate jars.
///
public abstract class AbstractJarGenerator extends AbstractGenerator {

    private final CachedStream<ResourceProvider> providers
        = new CachedStream<>();
    private Path destination;
    private Supplier<String> jarName
        = () -> project().name() + "-" + project().get(Version) + ".jar";
    private Stream<Entry<Name, String>> attributes;
    private final List<Stream<
            ? extends Map.Entry<Path, ? extends IOResource>>> additional
                = new ArrayList<>();
    private String mainClass;

    /// Instantiates a new library generator.
    ///
    /// @param project the project
    ///
    public AbstractJarGenerator(Project project) {
        super(project);
    }

    /// return the cached providers.
    ///
    /// @return the cached stream
    ///
    protected CachedStream<ResourceProvider> providers() {
        return providers;
    }

    /// Returns the destination directory. Defaults to "`libs`".
    ///
    /// @return the destination
    ///
    public Path destination() {
        return destination;
    }

    /// Sets the destination directory. The [Path] is resolved against
    /// the project's build directory (see [Project#buildDirectory]).
    ///
    /// @param destination the new destination
    /// @return the java compiler
    ///
    public AbstractJarGenerator destination(Path destination) {
        this.destination = destination;
        return this;
    }

    /// Returns the name of the generated jar file. Defaults to
    /// the project's name followed by its version and `.jar`.
    ///
    /// @return the string
    ///
    public String jarName() {
        return jarName.get();
    }

    /// Sets the supplier for obtaining the name of the generated jar file
    /// in [#provide].
    ///
    /// @param jarName the jar name
    /// @return the jar generator
    ///
    public AbstractJarGenerator jarName(Supplier<String> jarName) {
        this.jarName = jarName;
        return this;
    }

    /// Sets the name of the generated jar file.
    ///
    /// @param jarName the jar name
    /// @return the jar generator
    ///
    public AbstractJarGenerator jarName(String jarName) {
        return jarName(() -> jarName);
    }

    /// Returns the main class.
    ///
    /// @return the main class
    ///
    public String mainClass() {
        return mainClass;
    }

    /// Sets the main class.
    ///
    /// @param mainClass the new main class
    /// @return the jar generator for method chaining
    ///
    public AbstractJarGenerator mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /// Adds the given providers. Each provider will be asked to provide
    /// resources of type [ClassTree] and [JavaResourceTree] when
    /// [#provide] is invoked. All file trees returned in response
    /// are added to the library jar.
    ///
    /// @param providers the providers
    /// @return the library generator
    ///
    public AbstractJarGenerator addAll(Stream<ResourceProvider> providers) {
        this.providers.add(providers.filter(p -> !p.equals(this)));
        return this;
    }

    /// Add the given attributes to the manifest.
    ///
    /// @param attributes the attributes
    /// @return the library generator
    ///
    public AbstractJarGenerator
            attributes(Stream<Map.Entry<Attributes.Name, String>> attributes) {
        this.attributes = attributes;
        return this;
    }

    /// Adds the given providers, see [addAll].
    ///
    /// @param providers the providers
    /// @return the jar generator
    ///
    public AbstractJarGenerator add(ResourceProvider... providers) {
        addAll(Stream.of(providers));
        return this;
    }

    /// Adds resources to the jar that are not classpath entries.
    ///
    /// @param entries the entries
    /// @return the jar generator
    ///
    public AbstractJarGenerator add(
            Stream<? extends Map.Entry<Path, ? extends IOResource>> entries) {
        additional.add(entries);
        return this;
    }

    /// Builds the jar.
    ///
    /// @param jarResource the jar resource
    /// @param classpathElements the classpath elements
    ///
    @SuppressWarnings("PMD.CloseResource")
    protected void buildJar(JarFile jarResource,
            Resources<ClasspathElement> classpathElements) {
        log.info(() -> "Building jar in " + project().name());

        // Add main class if defined
        attributes(Map.of(Attributes.Name.MAIN_CLASS, mainClass()).entrySet()
            .stream());

        // Collect entries for jar
        var openJars = new ConcurrentHashMap<Path, java.util.jar.JarFile>();
        var entries = new ConcurrentHashMap<Path, Queue<IOResource>>();
        addEntries(entries, classpathElements.stream(), openJars);
        addAdditional(entries);
        resolveDuplicates(entries);

        // Add content to jar
        Manifest manifest = new Manifest();
        @SuppressWarnings("PMD.LooseCoupling")
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        this.attributes.forEach(e -> attributes.put(e.getKey(), e.getValue()));
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(
            jarResource.path(), CREATE, TRUNCATE_EXISTING), manifest)) {
            for (var entry : entries.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                var entryName
                    = StreamSupport.stream(entry.getKey().spliterator(), false)
                        .map(Path::toString).collect(Collectors.joining("/"));
                @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                JarEntry jarEntry = new JarEntry(entryName);
                jarEntry.setTime(entry.getValue().peek().asOf().toEpochMilli());
                jos.putNextEntry(jarEntry);
                try (var input = entry.getValue().peek().inputStream()) {
                    input.transferTo(jos);
                }
            }

            for (var jarFile : openJars.values()) {
                jarFile.close();
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private void addEntries(Map<Path, Queue<IOResource>> entries,
            Stream<? extends Resource> classpathElements,
            Map<Path, java.util.jar.JarFile> openJars) {
        classpathElements.parallel().forEach(cpe -> {
            if (cpe instanceof FileTree<?> fileTree) {
                addFileTree(entries, fileTree);
            } else if (cpe instanceof JarFile jarFile) {
                addJarFile(entries, jarFile, openJars);
            }
        });
    }

    private void addFileTree(Map<Path, Queue<IOResource>> entries,
            FileTree<?> fileTree) {
        var root = fileTree.root();
        fileTree.stream().forEach(file -> {
            var relPath = root.relativize(file.path());
            entries.computeIfAbsent(relPath,
                _ -> new ConcurrentLinkedQueue<IOResource>()).add(file);
        });
    }

    private void addJarFile(Map<Path, Queue<IOResource>> entries,
            JarFile jarFile, Map<Path, java.util.jar.JarFile> openJars) {
        @SuppressWarnings({ "PMD.PreserveStackTrace", "PMD.CloseResource" })
        java.util.jar.JarFile jar
            = openJars.computeIfAbsent(jarFile.path(), _ -> {
                try {
                    return new java.util.jar.JarFile(jarFile.path().toFile());
                } catch (IOException e) {
                    throw new BuildException("Cannot open resource " + jarFile
                        + ": " + e.getMessage());
                }

            });
        jar.stream().filter(Predicate.not(JarEntry::isDirectory))
            .filter(e -> {
                var segs = Path.of(e.getRealName()).iterator();
                if (segs.next().equals(Path.of("META-INF"))) {
                    segs.next();
                    return segs.hasNext();
                }
                return true;
            })
            .forEach(e -> {
                var relPath = Path.of(e.getRealName());
                entries.computeIfAbsent(relPath,
                    _ -> new ConcurrentLinkedQueue<IOResource>())
                    .add(new JarFileEntry(jar, e));
            });
    }

    private void addAdditional(Map<Path, Queue<IOResource>> entries) {
        additional.stream().flatMap(s -> s).forEach(entry -> {
            entries.computeIfAbsent(entry.getKey(),
                _ -> new ConcurrentLinkedQueue<IOResource>())
                .add(entry.getValue());
        });
    }

    /// Resolve duplicates. The default implementation outputs a warning
    /// and skips the duplicate entry. 
    ///
    /// @param entries the entries
    ///
    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition",
        "PMD.UselessPureMethodCall" })
    protected void resolveDuplicates(Map<Path, Queue<IOResource>> entries) {
        entries.entrySet().parallelStream().forEach(item -> {
            var queue = item.getValue();
            if (queue.size() == 1) {
                return;
            }
            var entryName = item.getKey();
            queue.stream().reduce((a, b) -> {
                log.warning(() -> "Entry " + entryName + " from " + a
                    + " duplicates entry from " + b + " and is skipped.");
                return a;
            });
        });
    }
}
