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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceRequest.Restriction.*;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.core.CachedStream;
import static org.jdrupes.builder.java.JavaTypes.*;

/// The Class AppJarGenerator.
///
public class AppJarGenerator extends AbstractGenerator<JarFile> {

    private final CachedStream<ResourceProvider<?>> providers
        = new CachedStream<>();
    private Path destination;
    private String mainClass;

    /// Instantiates a new app jar builder.
    ///
    /// @param project the project
    ///
    public AppJarGenerator(Project project) {
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
    public AppJarGenerator destination(Path destination) {
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
    /// @return the app jar builder for method chaining
    ///
    public AppJarGenerator mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /// Adds the given providers. Each provider is asked to provide
    /// resources of type `FileTree<ClassResource>` and
    /// `FileTree<ResourceResource>`. The resources from the provided
    /// [FileTree]s are added to the jar.
    ///
    /// @param providers the providers
    /// @return the app jar builder
    ///
    public AppJarGenerator add(ResourceProvider<?>... providers) {
        this.providers.add(Stream.of(providers));
        return this;
    }

    /// Adds the providers from the stream, see [add].
    ///
    /// @param providers the providers
    /// @return the app jar builder
    ///
    public AppJarGenerator
            addAll(Stream<? extends ResourceProvider<?>> providers) {
        this.providers.add(providers);
        return this;
    }

    @Override
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.CollapsibleIfStatements", "unchecked",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    public <T extends Resource> Stream<T>
            provide(ResourceRequest<T> requested) {
        if (!requested.acceptsResources(AppJarFileType)
            && !requested.accepts(Cleaniness)) {
            return Stream.empty();
        }

        // Prepare jar file
        var destDir = Optional.ofNullable(destination)
            .orElseGet(() -> project().buildDirectory().resolve("app"));
        if (!destDir.toFile().exists()) {
            if (!destDir.toFile().mkdirs()) {
                throw new BuildException("Cannot create directory " + destDir);
            }
        }
        var jarResource = project().create(AppJarFileType,
            destDir.resolve(project().name() + ".jar"));

        // Maybe only delete
        if (requested.accepts(Cleaniness)) {
            jarResource.delete();
            return Stream.empty();
        }

        // Make sure mainClass is set
        if (mainClass == null) {
            throw new BuildException("Main class must be set for "
                + name() + " in " + project());
        }

        // Get all content.
        log.fine(() -> "Getting app jar content for " + project().name());
        var toBeIncluded = project().create(ClasspathType)
            .addAll(providers.stream().map(p -> project().get(p,
                new ResourceRequest<ClasspathElement>(
                    new ResourceType<RuntimeResources>() {}, None)))
                .flatMap(s -> s));

        // Check if rebuild needed.
        if (jarResource.asOf().isAfter(toBeIncluded.asOf())) {
            return Stream.of((T) jarResource);
        }
        buildJar(jarResource, toBeIncluded);
        return Stream.of((T) jarResource);
    }

    private void buildJar(AppJarFile jarResource,
            Resources<ClasspathElement> classpathElements) {
        // Build jar
        log.info(() -> "Building application jar in " + project().name());
        var entries = new LinkedHashMap<Path, Path>();
        addEntries(entries, classpathElements.stream());

        // Add content to jar
        Manifest manifest = new Manifest();
        @SuppressWarnings("PMD.LooseCoupling")
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.MAIN_CLASS, mainClass);
        try (JarOutputStream jos = new JarOutputStream(
            Files.newOutputStream(jarResource.path()), manifest)) {
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
    }

    private void addEntries(Map<Path, Path> entries,
            Stream<? extends Resource> classpathElement) {
        // TODO: handle embedded jars.
        classpathElement.filter(fs -> fs instanceof FileTree)
            .map(fs -> (FileTree<?>) fs).forEach(fs -> {
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
