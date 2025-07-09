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
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.core.CachedStream;
import static org.jdrupes.builder.java.JavaTypes.*;

/// A [Generator] for uber jars.
///
/// The generator takes a simple approach:
/// 
///   * Add the content of the [ClasspathElement]s added with [add][#add]
///     or [addAll][#addAll] to the resulting uber jar.
///   * Filter out any direct child files of `META-INF`. These files often
///     contain information related to the origin jar that is not applicable
///     to the uber jar.
///   * Merge the files in `META-INF/services` that have the same name by
///     concatenating them.
///
/// Note that the output resource type of the uber jar generator matches
/// the resource type of its inputs, because uber jars can also be used
/// as [ClasspathElement]. Therefore, if you want to create an uber jar
/// from all resources provided by a project, you must not add the
/// generator to the project like this:
/// ```java
///     generator(UberJarGenerator::new).add(this); // Circular dependency
/// ```
///
/// This would add the project as provider and thus make the uber jar
/// generator as supplier to the project its own provider (via
/// [Project.provide][Project#provide]). Rather, you have to use this
/// slightly more complicated approach to adding providers to the uber
/// jar generator:
/// ```java
///     generator(UberJarGenerator::new)
///         .addAll(providers(EnumSet.of(Forward, Expose, Supply)));
/// ```
/// This requests the same providers from the project as 
/// [Project.provide][Project#provide] does, but allows the uber jar
/// generator's [addAll][#addAll] method to filter out the uber jar
/// generator itself from the providers. The given intends can
/// vary depending on the requirements.
///
/// If you don't want the generated uber jar to be available to other
/// generators of your project, you can also add it to a project like this:
/// ```java
///     dependency(new UberJarGenerator(this)
///         .addAll(providers(EnumSet.of(Forward, Expose, Supply))), Intend.Forward)
/// ```
///
/// Of course, the easiest thing to do is separate the generation of
/// class trees or library jars from the generation of the uber jar by
/// generating the uber jar in a project of its own. Often the root
/// project can be used for this purpose.  
///
public class UberJarGenerator extends AbstractGenerator<JarFile> {

    private final CachedStream<ResourceProvider<?>> providers
        = new CachedStream<>();
    private Path destination;
    private String mainClass;

    /// Instantiates a new uber jar generator.
    ///
    /// @param project the project
    ///
    public UberJarGenerator(Project project) {
        super(project);
    }

    /// Returns the destination directory. Defaults to "`app`".
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
    public UberJarGenerator destination(Path destination) {
        this.destination = destination;
        return this;
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
    /// @return the uber jar generator for method chaining
    ///
    public UberJarGenerator mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /// Adds the given providers. Each provider will be asked to provide
    /// resources of type [ClasspathElement] when [#provide] is invoked.
    /// All file trees and the content of all jars returned in response
    /// are added to the uber jar.
    ///
    /// Because an uber jar is also a [ClasspathElement] this
    /// [UberJarGenerator] is also a provider of [ClasspathElement]s.
    /// To avoid loops, `this` is therefore automatically filtered
    /// from the given providers.
    ///
    /// @param providers the providers
    /// @return the uber jar generator
    ///
    public UberJarGenerator addAll(Stream<ResourceProvider<?>> providers) {
        this.providers.add(providers.filter(p -> !p.equals(this)));
        return this;
    }

    /// Adds the given providers, see [addAll].
    ///
    /// @param providers the providers
    /// @return the uber jar generator
    ///
    public UberJarGenerator add(ResourceProvider<?>... providers) {
        addAll(Stream.of(providers));
        return this;
    }

    @Override
    @SuppressWarnings({ "PMD.CollapsibleIfStatements", "unchecked" })
    public <T extends Resource> Stream<T>
            provide(ResourceRequest<T> requested) {
        if (!requested.includes(AppJarFileType)
            && !requested.includes(Cleaniness)) {
            return Stream.empty();
        }

        // Prepare jar file
        boolean wantAppJar
            = AppJarFileType.isAssignableFrom(requested.type().containedType());
        var destDir = Optional.ofNullable(destination)
            .orElseGet(() -> project().buildDirectory().resolve(
                wantAppJar ? "app" : "libs"));
        if (!destDir.toFile().exists()) {
            if (!destDir.toFile().mkdirs()) {
                throw new BuildException("Cannot create directory " + destDir);
            }
        }

        // Maybe only delete
        if (requested.includes(Cleaniness)) {
            project().resource(JarFileType,
                destDir.resolve(project().name() + ".jar")).delete();
            return Stream.empty();
        }

        // Make sure mainClass is set
        if (wantAppJar && mainClass == null) {
            throw new BuildException("Main class must be set for "
                + name() + " in " + project());
        }

        // Get all content.
        log.fine(() -> "Getting uber jar content for " + project().name());
        @SuppressWarnings("PMD.UseDiamondOperator")
        var toBeIncluded = project().resource(ClasspathType)
            .addAll(project().invokeProviders(providers.stream(),
                new ResourceRequest<ClasspathElement>(
                    new ResourceType<RuntimeResources>() {})));
        log.fine(() -> "Uber jar content: " + toBeIncluded.stream()
            .map(e -> project().relativize(e.toPath()).toString())
            .collect(Collectors.joining(":")));

        // Check if rebuild needed.
        var jarResource = (JarFile) project().resource(requested.type()
            .containedType(), destDir.resolve(project().name() + ".jar"));
        if (jarResource.asOf().isAfter(toBeIncluded.asOf())) {
            return Stream.of((T) jarResource);
        }
        buildJar(jarResource, toBeIncluded);
        return Stream.of((T) jarResource);
    }

    private void buildJar(JarFile jarResource,
            Resources<ClasspathElement> classpathElements) {
        // Build jar
        log.info(() -> "Building application jar in " + project().name());
        var openJars = new ConcurrentHashMap<Path, java.util.jar.JarFile>();
        var entries = new ConcurrentHashMap<Path, Queue<Noted>>();
        addEntries(entries, classpathElements.stream(), openJars);
        resolveDuplicates(entries);

        // Add content to jar
        Manifest manifest = new Manifest();
        @SuppressWarnings("PMD.LooseCoupling")
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.MAIN_CLASS, mainClass);
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
        @SuppressWarnings("PMD.PreserveStackTrace")
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
        "PMD.PreserveStackTrace" })
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
            return new ByteArrayInputStream(content.toString().getBytes());
        }

        @Override
        public long lastModified() {
            return Instant.now().toEpochMilli();
        }

    }

}
