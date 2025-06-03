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
import java.util.ArrayList;
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
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFile;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractGenerator;

/// The Class AppJarBuilder.
///
public class AppJarBuilder extends AbstractGenerator<JarFile> {

    private final List<Stream<? extends ResourceProvider<?>>> providers
        = new ArrayList<>();

    /// Instantiates a new app jar builder.
    ///
    /// @param project the project
    ///
    public AppJarBuilder(Project project) {
        super(project);
    }

    /// Adds the given providers. Each provider is asked to provide
    /// resources of type `FileTree<ClassResource>` and
    /// `FileTree<ResourceResource>`. The resources from the provided
    /// [FileTree]s are added to the jar.
    ///
    /// @param providers the providers
    /// @return the app jar builder
    ///
    public AppJarBuilder add(ResourceProvider<?>... providers) {
        this.providers.add(Stream.of(providers));
        return this;
    }

    /// Adds the providers from the stream, see [add].
    ///
    /// @param providers the providers
    /// @return the app jar builder
    ///
    public AppJarBuilder
            addAll(Stream<? extends ResourceProvider<?>> providers) {
        this.providers.add(providers);
        return this;
    }

    /// Provides the jar.
    ///
    /// @param <T> the generic type
    /// @param requested the requested
    /// @return the stream
    ///
    @Override
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.CollapsibleIfStatements", "unchecked",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    public <T extends Resource> Stream<T>
            provide(ResourceRequest<T> requested) {
        if (!requested.type().isAssignableFrom(new ResourceType<JarFile>() {
        })) {
            return Stream.empty();
        }

        // Get all content.
        log.fine(() -> "Getting app jar content for " + project().name());
        Resources<FileTree<? extends Resource>> fileTrees
            = project().newResources(FileTree.class);
        providers.stream().flatMap(s -> s).forEach(provider -> {
            fileTrees.addAll(project().get(
                provider, new ResourceRequest<>(JavaConsts.JAVA_CLASS_FILES)));
            fileTrees.addAll(project().get(
                provider, new ResourceRequest<>(
                    new ResourceType<FileTree<ResourceFile>>() {
                    })));
        });

        // Prepare jar file
        var destDir = project().buildDirectory().resolve("app");
        if (!destDir.toFile().exists()) {
            if (!destDir.toFile().mkdirs()) {
                throw new BuildException("Cannot create directory " + destDir);
            }
        }
        var jarResource = project()
            .newFileResource(JarFile.class,
                destDir.resolve(project().name() + ".jar"));
        if (jarResource.asOf().isAfter(fileTrees.asOf())) {
            return Stream.of((T) jarResource);
        }

        // Build jar
        log.info(() -> "Building application jar in " + project().name());
        var entries = new LinkedHashMap<Path, Path>();
        addEntries(entries, fileTrees.stream());

        // Add content to jar
        Manifest manifest = new Manifest();
        @SuppressWarnings("PMD.LooseCoupling")
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
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

        // The result is the jar.
        return Stream.of((T) jarResource);
    }

    private void addEntries(Map<Path, Path> entries,
            Stream<? extends Resource> fileSets) {
        fileSets.filter(fs -> fs instanceof FileTree)
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
