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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.*;
import java.time.Instant;
import java.util.Comparator;
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
import org.jdrupes.builder.api.Project;
import static org.jdrupes.builder.api.Project.Properties.*;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.core.CachedStream;

/// A base class for generators tha generate jars.
///
public abstract class JarGenerator extends AbstractGenerator {

    private final CachedStream<ResourceProvider> providers
        = new CachedStream<>();
    private Path destination;
    private Supplier<String> jarName
        = () -> project().name() + "-" + project().get(Version) + ".jar";
    private Stream<Entry<Name, String>> attributes;

    /// Instantiates a new library generator.
    ///
    /// @param project the project
    ///
    public JarGenerator(Project project) {
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
    public JarGenerator destination(Path destination) {
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
    /// @return the uber jar generator
    ///
    public JarGenerator jarName(Supplier<String> jarName) {
        this.jarName = jarName;
        return this;
    }

    /// Sets the name of the generated jar file.
    ///
    /// @param jarName the jar name
    /// @return the uber jar generator
    ///
    public JarGenerator jarName(String jarName) {
        return jarName(() -> jarName);
    }

    /// Adds the given providers. Each provider will be asked to provide
    /// resources of type [ClassTree] and [JavaResourceTree] when
    /// [#provide] is invoked. All file trees returned in response
    /// are added to the library jar.
    ///
    /// @param providers the providers
    /// @return the library generator
    ///
    public JarGenerator addAll(Stream<ResourceProvider> providers) {
        this.providers.add(providers.filter(p -> !p.equals(this)));
        return this;
    }

    /// Add the given attributes to the manifest.
    ///
    /// @param attributes the attributes
    /// @return the library generator
    ///
    public JarGenerator
            attributes(Stream<Map.Entry<Attributes.Name, String>> attributes) {
        this.attributes = attributes;
        return this;
    }

    /// Adds the given providers, see [addAll].
    ///
    /// @param providers the providers
    /// @return the uber jar generator
    ///
    public JarGenerator add(ResourceProvider... providers) {
        addAll(Stream.of(providers));
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
        // Build jar
        log.info(() -> "Building jar in " + project().name());
        var openJars = new ConcurrentHashMap<Path, java.util.jar.JarFile>();
        var entries = new ConcurrentHashMap<Path, Queue<Noted>>();
        addEntries(entries, classpathElements.stream(), openJars);
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
                jarEntry.setTime(entry.getValue().peek().lastModified());
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

    private void addEntries(Map<Path, Queue<Noted>> entries,
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

    private void addFileTree(Map<Path, Queue<Noted>> entries,
            FileTree<?> fileTree) {
        var root = fileTree.root();
        fileTree.stream().forEach(file -> {
            var relPath = root.relativize(file.path());
            entries.computeIfAbsent(relPath,
                _ -> new ConcurrentLinkedQueue<Noted>())
                .add(new NotedFileTreeEntry(root, file.path()));
        });
    }

    private void addJarFile(Map<Path, Queue<Noted>> entries, JarFile jarFile,
            Map<Path, java.util.jar.JarFile> openJars) {
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
                    _ -> new ConcurrentLinkedQueue<Noted>())
                    .add(new NotedJarFileEntry(jarFile.path(), jar, e));
            });
    }

    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition",
        "PMD.PreserveStackTrace", "PMD.UselessPureMethodCall" })
    private void resolveDuplicates(Map<Path, Queue<Noted>> entries) {
        entries.entrySet().parallelStream().forEach(entry -> {
            var queue = entry.getValue();
            if (queue.size() == 1) {
                return;
            }
            var path = entry.getKey();
            if (path.startsWith("META-INF/services")) {
                var combined = new NotedServicesEntry();
                for (var noted : queue) {
                    try {
                        combined.add(noted.inputStream());
                    } catch (IOException e) {
                        throw new BuildException("Cannot read " + path
                            + " in " + noted.origin());
                    }
                }
                queue.clear();
                queue.add(combined);
                return;
            }
            if (path.startsWith("META-INF")) {
                queue.clear();
            }
            queue.stream().sorted(Comparator.comparing(Noted::origin))
                .reduce((a, b) -> {
                    log.warning(() -> "Entry " + path + " from "
                        + project().rootProject().relativize(b.origin())
                        + " duplicates entry from "
                        + project().rootProject().relativize(a.origin())
                        + " and is skipped.");
                    return a;
                });
        });
    }

    /// A Noted entry.
    ///
    private interface Noted {

        /// Origin.
        ///
        /// @return the path
        ///
        Path origin();

        /// Input stream.
        ///
        /// @return the input stream
        /// @throws IOException Signals that an I/O exception has occurred.
        ///
        InputStream inputStream() throws IOException;

        /// Last modified.
        ///
        /// @return the long
        ///
        long lastModified();
    }

    /// The Class NotedFileTreeEntry.
    ///
    private class NotedFileTreeEntry implements Noted {
        private final Path root;
        private final Path entry;

        /// Instantiates a new noted file tree entry.
        ///
        /// @param root the root
        /// @param entry the entry
        ///
        public NotedFileTreeEntry(Path root, Path entry) {
            this.root = root;
            this.entry = entry;
        }

        @Override
        public Path origin() {
            return root;
        }

        @Override
        public InputStream inputStream() throws IOException {
            return Files.newInputStream(root.resolve(entry));
        }

        @Override
        public long lastModified() {
            return root.resolve(entry).toFile().lastModified();
        }

    }

    /// The Class NotedJarFileEntry.
    ///
    private class NotedJarFileEntry implements Noted {
        private final Path jarPath;
        private final java.util.jar.JarFile jarFile;
        private final JarEntry entry;

        /// Instantiates a new noted jar file entry.
        ///
        /// @param jarPath the jar path
        /// @param jarFile the jar file
        /// @param entry the entry
        ///
        public NotedJarFileEntry(Path jarPath, java.util.jar.JarFile jarFile,
                JarEntry entry) {
            this.jarPath = jarPath;
            this.entry = entry;
            this.jarFile = jarFile;
        }

        @Override
        public Path origin() {
            return jarPath;
        }

        @Override
        public InputStream inputStream() throws IOException {
            return jarFile.getInputStream(entry);
        }

        @Override
        public long lastModified() {
            return entry.getLastModifiedTime().toMillis();
        }

    }

    /// The Class NotedServicesEntry.
    ///
    private final class NotedServicesEntry implements Noted {

        @SuppressWarnings("PMD.AvoidStringBufferField")
        private final StringBuilder content = new StringBuilder();

        /// Adds the input.
        ///
        /// @param input the input
        /// @throws IOException Signals that an I/O exception has occurred.
        ///
        public void add(InputStream input) throws IOException {
            try (InputStream toRead = input) {
                new String(toRead.readAllBytes(), StandardCharsets.UTF_8)
                    .lines().filter(Predicate.not(String::isBlank))
                    .forEach(l -> content.append(l).append('\n'));
            }
        }

        @Override
        public Path origin() {
            return Path.of("");
        }

        @Override
        public InputStream inputStream() throws IOException {
            return new ByteArrayInputStream(
                content.toString().getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public long lastModified() {
            return Instant.now().toEpochMilli();
        }

    }

}
