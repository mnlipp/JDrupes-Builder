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

package org.jdrupes.builder.distribution;

import com.google.common.flogger.FluentLogger;
import io.github.azagniotov.matcher.AntPathMatcher;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ConfigurationException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.InputResource;
import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.java.ClassTree;
import org.jdrupes.builder.java.ClasspathElement;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.java.JarFileEntry;
import org.jdrupes.builder.java.JavaResourceTree;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.LibraryBuilder;
import org.jdrupes.builder.java.LibraryJarFile;
import org.jdrupes.builder.java.ServicesEntryResource;
import org.jdrupes.builder.mvnrepo.MvnRepoJarFile;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.mvnrepo.MvnRepoResource;

/// A [Generator] for uber jars.
///
/// Depending on the request, the generator provides one of two resource
/// types.
/// 
/// 1. A [JarFile]. This type of resource is also returned if a more
///    general [ResourceType] such as [ClasspathElement] is requested.
///
/// 2. An [AppJarFile]. When this special [JarFile] type is requested, the
///    generator requires a main class to be configured.
///
/// The generator takes the following approach:
/// 
///   * Request resources of type [ClasspathElement] with all intents from
///     the added providers. Add the class and resource trees to the sources
///     to be processed. JAR files are handled differently depending on their
///     origin. The content of JAR files that are not retrieved from a Maven
///     repository is added to the sources to be processed. For
///     [MvnRepoJarFile]s (i.e. JAR files from a Maven repository) only the
///     [MvnRepoResource] reference is collected.
///   * Use all [MvnRepoResource]s obtained in the previous step for a
///     dependency resolution. Add the content from the resulting JAR
///     files to the sources to be processed.
///   * Add resources from the sources to the uber jar. Merge the files in
///     `META-INF/services/` that have the same name by concatenating them.
///   * Filter out any other duplicate files under `META-INF`.
///     These files often contain information related to the origin jar
///     that is not applicable to the uber jar.
///   * Filter out any module-info.class entries.
///
/// The resource type of the uber jar builder's output is one
/// of the resource types of its inputs, because uber jars can also be used
/// as [ClasspathElement]. Therefore, you cannot add a uber jar builder
/// to the project like this:
/// ```java
///     generator(UberJarBuilder::new).addFrom(this); // Circular dependency
/// ```
///
/// This would add the project as provider and thus make the uber jar
/// builder's result a uber jar builder's source (via
/// [Project.resources][Project#resources]). Instead use the following
/// approach:
/// ```java
///     generator(UberJarGenerator::new)
///         .addFrom(providers().select(Forward, Expose, Supply));
/// ```
/// 
/// This requests the same providers from the project as 
/// [Project.resources][Project#resources] would, but allows the uber jar
/// builder's [addFrom] method to filter out the uber jar
/// builder itself from the providers. The given intents can
/// vary depending on the requirements.
///
/// If the generated uber jar should not be visible to the project's other
/// generators, you can also add it like this:
/// ```java
///     dependency(new UberJarGenerator(this).addFrom(
///         providers(EnumSet.of(Forward, Expose, Supply))), Intent.Forward)
/// ```
///
/// In most cases, the simplest solution is to generate the uber jar
/// in a separate project, typically the parent project. This cleanly
/// separates the generation of class and resource trees and library jars
/// from the generation of the uber jar.
///
public class UberJarBuilder extends LibraryBuilder {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final AntPathMatcher pathMatcher
        = new AntPathMatcher.Builder().build();
    private Map<Path, java.util.jar.JarFile> openJars = Map.of();
    private Predicate<Resource> resourceFilter = _ -> true;
    private final List<String> ignoredDuplicates = new ArrayList<>();

    /// Instantiates a new uber jar generator.
    ///
    /// @param project the project
    ///
    public UberJarBuilder(Project project) {
        super(Objects.requireNonNull(project));
    }

    @Override
    public UberJarBuilder name(String name) {
        rename(Objects.requireNonNull(name));
        return this;
    }

    /// Ignore duplicates matching the given glob patterns when merging.
    ///
    /// @param patterns the patterns
    /// @return the uber jar builder
    ///
    public UberJarBuilder ignoreDuplicates(String... patterns) {
        ignoredDuplicates.addAll(Arrays.asList(patterns));
        return this;
    }

    @Override
    protected void collectFromProviders(
            Map<Path, Resources<InputResource>> contents) {
        Resources<MvnRepoResource> repoRefs
            = Resources.of(new ResourceType<>() {});
        openJars = new ConcurrentHashMap<>();
        contentProviders().stream().filter(p -> !p.equals(this))
            .map(p -> p.resources(of(ClasspathElementType).usingAll()))
            // Terminate to trigger all future stream evaluations before
            // starting to process the results. Then collect in parallel
            .toList().stream().flatMap(s -> s).toList().parallelStream()
            .filter(resourceFilter::test).forEach(cpe -> {
                if (cpe instanceof FileTree<?> fileTree) {
                    collect(contents, fileTree);
                    return;
                }
                if (cpe instanceof JarFile jarFile) {
                    if (jarFile instanceof MvnRepoJarFile repoFile) {
                        // Resolve JAR files from Maven repositories, see below
                        repoRefs.add(repoFile.reference());
                    } else {
                        addJarFile(contents, jarFile, openJars);
                    }
                }
            });

        // Jar files from Maven repositories must be resolved before
        // they can be added to the uber jar to avoid duplicates.
        var lookup = new MvnRepoLookup();
        lookup.resolve(repoRefs.stream());
        project().context().resources(lookup, of(ClasspathElementType)
            .using(Consume, Reveal, Supply, Expose, Forward))
            .parallel().filter(resourceFilter::test).forEach(cpe -> {
                if (cpe instanceof MvnRepoJarFile jarFile) {
                    addJarFile(contents, jarFile, openJars);
                }
            });
    }

    /// Apply the given filter to the resources obtained from the provider.
    /// The resources can be [ClasspathElement]s or [MvnRepoResource]s.
    /// This may be required to avoid warnings about duplicates if e.g.
    /// a sub-project provides generated resources both as
    /// [ClassTree]/[JavaResourceTree] and as [LibraryJarFile].
    ///
    /// @param filter the filter. Returns `true` for resources to be
    /// included.
    /// @return the uber jar generator
    ///
    public UberJarBuilder resourceFilter(Predicate<Resource> filter) {
        resourceFilter = Objects.requireNonNull(filter);
        return this;
    }

    private void addJarFile(Map<Path, Resources<InputResource>> entries,
            JarFile jarFile, Map<Path, java.util.jar.JarFile> openJars) {
        @SuppressWarnings({ "PMD.CloseResource" })
        java.util.jar.JarFile jar
            = openJars.computeIfAbsent(jarFile.path(), _ -> {
                try {
                    return new java.util.jar.JarFile(jarFile.path().toFile());
                } catch (IOException e) {
                    throw new BuildException().from(this).cause(e);
                }
            });
        jar.stream().filter(Predicate.not(JarEntry::isDirectory))
            .filter(e -> !Path.of(e.getName())
                .endsWith(Path.of("module-info.class")))
            .filter(e -> {
                // Filter top-level entries in META-INF/
                var segs = Path.of(e.getRealName()).iterator();
                if (segs.next().equals(Path.of("META-INF"))) {
                    segs.next();
                    return segs.hasNext();
                }
                return true;
            }).forEach(e -> {
                var relPath = Path.of(e.getRealName());
                entries.computeIfAbsent(relPath,
                    _ -> Resources.with(InputResource.class))
                    .add(new JarFileEntry(jar, e));
            });
    }

    @SuppressWarnings({ "PMD.UselessPureMethodCall",
        "PMD.AvoidLiteralsInIfCondition" })
    @Override
    protected void
            resolveDuplicates(Map<Path, Resources<InputResource>> entries) {
        entries.entrySet().parallelStream().forEach(item -> {
            var entryName = item.getKey();
            var candidates = item.getValue();
            if (candidates.stream().count() == 1) {
                return;
            }
            if (entryName.startsWith("META-INF/services")) {
                var combined = new ServicesEntryResource();
                candidates.stream().forEach(service -> {
                    try {
                        combined.add(service);
                    } catch (IOException e) {
                        throw new BuildException().from(this).cause(e);
                    }
                });
                candidates.clear();
                candidates.add(combined);
                return;
            }
            if (entryName.startsWith("META-INF")) {
                candidates.clear();
            }
            if (ignoredDuplicates.stream()
                .map(p -> pathMatcher.isMatch(p, entryName.toString()))
                .filter(Boolean::booleanValue).findFirst().isPresent()) {
                return;
            }
            candidates.stream().reduce((a, b) -> {
                logger.atWarning().log("Entry %s from %s duplicates"
                    + " entry from %s and is skipped.", entryName, a, b);
                return a;
            });
        });
    }

    @Override
    @SuppressWarnings({ "PMD.CollapsibleIfStatements", "unchecked",
        "PMD.CloseResource", "PMD.UseTryWithResources",
        "PMD.CognitiveComplexity", "PMD.CyclomaticComplexity" })
    protected <T extends Resource> Collection<T>
            doProvide(ResourceRequest<T> request) {
        if (!request.accepts(AppJarFileType)
            && !request.accepts(CleanlinessType)) {
            return Collections.emptyList();
        }

        // Maybe only delete
        if (request.accepts(CleanlinessType)) {
            destination().resolve(jarName()).toFile().delete();
            return Collections.emptyList();
        }

        // Make sure mainClass is set for app jar
        if (request.isFor(AppJarFileType) && mainClass() == null) {
            throw new ConfigurationException().from(this)
                .message("Main class must be set for %s", name());
        }

        // Upgrade to most specific type to avoid duplicate generation
        if (mainClass() != null && !request.type().equals(AppJarFileType)) {
            return (Collection<T>) context()
                .resources(this, project().of(AppJarFileType)).toList();
        }
        if (mainClass() == null && !request.type().equals(JarFileType)) {
            return (Collection<T>) context()
                .resources(this, project().of(JarFileType)).toList();
        }

        // Prepare jar file
        var destDir = destination();
        if (!destDir.toFile().exists()) {
            if (!destDir.toFile().mkdirs()) {
                throw new ConfigurationException().from(this)
                    .message("Cannot create directory " + destDir);
            }
        }
        var jarResource = request.isFor(AppJarFileType)
            ? AppJarFile.of(destDir.resolve(jarName()))
            : LibraryJarFile.of(destDir.resolve(jarName()));
        try {
            buildJar(jarResource);
        } finally {
            // buidJar indirectly calls collectFromProviders which opens
            // resources that are used in buildJar. Close them now.
            for (var jarFile : openJars.values()) {
                try {
                    jarFile.close();
                } catch (IOException e) { // NOPMD
                    // Ignore, just trying to be nice.
                }
            }
        }
        return List.of((T) jarResource);
    }
}
