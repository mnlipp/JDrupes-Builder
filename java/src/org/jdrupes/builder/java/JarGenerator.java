/*
 * JDrupes Builder
 * Copyright (C) 2025, 2026 Michael N. Lipp
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

import com.google.common.flogger.FluentLogger;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
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
import org.jdrupes.builder.api.ResourceProviderSpi;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.core.StreamCollector;

/// A general purpose generator for jars. All contents must be added
/// explicitly using [#add(Entry...)] or [#add(FileTree...)].
///
@SuppressWarnings({ "PMD.CouplingBetweenObjects", "PMD.TooManyMethods" })
public class JarGenerator extends AbstractGenerator {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final ResourceType<? extends JarFile> jarType;
    private Supplier<Path> destination
        = () -> project().buildDirectory().resolve("libs");
    private Supplier<String> jarName
        = () -> project().name() + "-" + project().get(Version) + ".jar";
    private final StreamCollector<Entry<Name, String>> attributes
        = StreamCollector.cached();
    private final StreamCollector<
            Map.Entry<Path, ? extends IOResource>> entryStreams
                = StreamCollector.cached();
    private final StreamCollector<FileTree<?>> fileTrees
        = StreamCollector.cached();

    /// Initializes a new library generator.
    ///
    /// @param project the project
    /// @param jarType the type of jar that the generator generates
    ///
    public JarGenerator(Project project,
            ResourceType<? extends JarFile> jarType) {
        super(project);
        this.jarType = jarType;
    }

    @Override
    public JarGenerator name(String name) {
        rename(name);
        return this;
    }

    /// Returns the destination directory. Defaults to sub directory
    /// `libs` in the project's build directory
    /// (see [Project#buildDirectory]).
    ///
    /// @return the destination
    ///
    public Path destination() {
        return destination.get();
    }

    /// Sets the destination directory. The [Path] is resolved against
    /// the project's build directory (see [Project#buildDirectory]).
    ///
    /// @param destination the new destination
    /// @return the jar generator
    ///
    public JarGenerator destination(Path destination) {
        this.destination
            = () -> project().buildDirectory().resolve(destination);
        return this;
    }

    /// Sets the destination directory.
    ///
    /// @param destination the new destination
    /// @return the jar generator
    ///
    public JarGenerator destination(Supplier<Path> destination) {
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
    /// in [ResourceProviderSpi#provide].
    ///
    /// @param jarName the jar name
    /// @return the jar generator
    ///
    public JarGenerator jarName(Supplier<String> jarName) {
        this.jarName = jarName;
        return this;
    }

    /// Sets the name of the generated jar file.
    ///
    /// @param jarName the jar name
    /// @return the jar generator
    ///
    public JarGenerator jarName(String jarName) {
        return jarName(() -> jarName);
    }

    /// Add the given attributes to the manifest.
    ///
    /// @param attributes the attributes
    /// @return the library generator
    ///
    public JarGenerator
            attributes(Stream<Map.Entry<Attributes.Name, String>> attributes) {
        this.attributes.add(attributes);
        return this;
    }

    /// Add the given attributes to the manifest.
    ///
    /// @param attributes the attributes
    /// @return the library generator
    ///
    @SafeVarargs
    public final JarGenerator
            attributes(Map.Entry<Attributes.Name, String>... attributes) {
        this.attributes.add(Arrays.stream(attributes));
        return this;
    }

    /// Adds single resources to the jar. Each entry is added to the
    /// jar as entry with the name passed in the key attribute of the
    /// `Map.Entry` with the content from the [IOResource] in the
    /// value attribute.
    ///
    /// @param entries the entries
    /// @return the jar generator
    ///
    public JarGenerator addEntries(
            Stream<? extends Map.Entry<Path, ? extends IOResource>> entries) {
        entryStreams.add(entries);
        return this;
    }

    /// Adds the given [FileTree]s. Each file in the tree will be added
    /// as an entry using its relative path in the tree as name.  
    ///
    /// @param trees the trees
    /// @return the jar generator
    ///
    public JarGenerator addTrees(Stream<? extends FileTree<?>> trees) {
        fileTrees.add(trees);
        return this;
    }

    /// Convenience method for adding entries, see [#addTrees(Stream)].
    ///
    /// @param trees the trees
    /// @return the jar generator
    ///
    public JarGenerator add(FileTree<?>... trees) {
        addTrees(Arrays.stream(trees));
        return this;
    }

    /// Convenience method for adding a single entry, see [#addEntries(Stream)].
    ///
    /// @param entries the entry
    /// @return the jar generator
    ///
    public JarGenerator add(@SuppressWarnings("unchecked") Map.Entry<Path,
            ? extends IOResource>... entries) {
        addEntries(Arrays.stream(entries));
        return this;
    }

    /// Builds the jar.
    ///
    /// @param jarResource the jar resource
    ///
    @SuppressWarnings("PMD.ConfusingTernary")
    protected void buildJar(JarFile jarResource) {
        // Collect entries for jar from all sources
        var contents = new ConcurrentHashMap<Path, Resources<IOResource>>();
        collectContents(contents);
        resolveDuplicates(contents);

        // Check if rebuild needed (requires manifest check).
        var oldManifest = Option.of(jarResource)
            .filter(jar -> jar.path().toFile().canRead())
            .flatMap(jr -> Try.withResources(
                () -> new java.util.jar.JarFile(jr.path().toFile()))
                .of(jar -> Try.of(jar::getManifest).toOption()
                    .flatMap(Option::of))
                .toOption().flatMap(m -> m))
            .getOrElse(Manifest::new);
        Manifest manifest = createManifest(contents.values());
        if (!manifest.equals(oldManifest)) {
            logger.atFine().log("Rebuilding %s, manifest changed", jarName());
        } else {
            // manifest unchanged, check timestamps
            var newer = contents.values().stream()
                .map(r -> r.stream().findFirst().stream()).flatMap(s -> s)
                .filter(r -> r.asOf().isAfter(jarResource.asOf())).findAny();
            if (newer.isEmpty()) {
                logger.atFine().log("Existing %s is up to date.", jarName());
                return;
            }
            logger.atFine().log(
                "Rebuilding %s, is older than %s", jarName(), newer.get());
        }
        writeJar(jarResource, contents, manifest);
    }

    private Manifest
            createManifest(Collection<Resources<IOResource>> collection) {
        Manifest manifest = new Manifest();
        @SuppressWarnings("PMD.LooseCoupling")
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        this.attributes.stream()
            .forEach(e -> attributes.put(e.getKey(), e.getValue()));
        return manifest;
    }

    private void writeJar(JarFile jarResource,
            Map<Path, Resources<IOResource>> contents, Manifest manifest) {
        // Write jar file
        logger.atInfo().log("Building %s in %s", jarName(), project().name());
        try {
            // Allow continued use of existing jar if open (POSIX only)
            Files.deleteIfExists(jarResource.path());
        } catch (IOException e) { // NOPMD
        }
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(
            jarResource.path(), CREATE, TRUNCATE_EXISTING), manifest)) {
            for (var entry : contents.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                var entryName
                    = StreamSupport.stream(entry.getKey().spliterator(), false)
                        .map(Path::toString).collect(Collectors.joining("/"));
                @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                JarEntry jarEntry = new JarEntry(entryName);
                jarEntry.setTime(entry.getValue().stream().findFirst().get()
                    .asOf().toEpochMilli());
                jos.putNextEntry(jarEntry);
                try (var input = entry.getValue().stream().findFirst().get()
                    .inputStream()) {
                    input.transferTo(jos);
                }
            }

        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /// Add the contents from the added streams as preliminary jar
    /// entries. Must be overridden by derived classes that define
    /// additional ways to provide contents. The overriding method
    /// must invoke `super.collectEntries(...)`.
    ///
    /// @param contents the preliminary contents
    ///
    protected void collectContents(Map<Path, Resources<IOResource>> contents) {
        entryStreams.stream().forEach(entry -> {
            contents.computeIfAbsent(entry.getKey(),
                _ -> project().newResource(IOResourcesType))
                .add(entry.getValue());
        });
        fileTrees.stream().parallel()
            .forEach(t -> collect(contents, t));
    }

    /// Adds the resources from the given file tree to the given contents.
    /// May be used by derived classes while collecting contents for
    /// the jar.
    ///
    /// @param collected the preliminary contents
    /// @param fileTree the file tree
    ///
    protected void collect(Map<Path, Resources<IOResource>> collected,
            FileTree<?> fileTree) {
        var root = fileTree.root();
        fileTree.stream().forEach(file -> {
            var relPath = root.relativize(file.path());
            collected.computeIfAbsent(relPath,
                _ -> project().newResource(IOResourcesType)).add(file);
        });
    }

    /// Resolve duplicates. The default implementation outputs a warning
    /// and skips the duplicate entry. 
    ///
    /// @param entries the entries
    ///
    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition",
        "PMD.UselessPureMethodCall" })
    protected void resolveDuplicates(
            Map<Path, Resources<IOResource>> entries) {
        entries.entrySet().parallelStream().forEach(item -> {
            var resources = item.getValue();
            if (resources.stream().count() == 1) {
                return;
            }
            var entryName = item.getKey();
            resources.stream().reduce((a, b) -> {
                logger.atWarning().log(
                    "Entry %s from %s duplicates entry from %s and is skipped.",
                    entryName, a, b);
                return a;
            });
        });
    }

    @Override
    @SuppressWarnings({ "PMD.CollapsibleIfStatements", "unchecked" })
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.accepts(jarType)
            && !requested.accepts(CleanlinessType)) {
            return Stream.empty();
        }

        // Prepare jar file
        var destDir = destination();
        if (!destDir.toFile().exists()) {
            if (!destDir.toFile().mkdirs()) {
                throw new BuildException("Cannot create directory " + destDir);
            }
        }
        var jarResource = project().newResource(jarType,
            destDir.resolve(jarName()));

        // Maybe only delete
        if (requested.accepts(CleanlinessType)) {
            jarResource.delete();
            return Stream.empty();
        }

        // Upgrade to most specific type to avoid duplicate generation
        if (!requested.type().equals(jarType)) {
            return (Stream<T>) context().resources(this, project().of(jarType));
        }

        buildJar(jarResource);
        return Stream.of((T) jarResource);
    }
}
