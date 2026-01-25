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

package org.jdrupes.builder.uberjar;

import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.IOResource;
import org.jdrupes.builder.api.Intent;
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
import org.jdrupes.builder.java.LibraryGenerator;
import org.jdrupes.builder.java.LibraryJarFile;
import org.jdrupes.builder.java.ServicesEntryResource;
import org.jdrupes.builder.mvnrepo.MvnRepoJarFile;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.mvnrepo.MvnRepoResource;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

/// A [Generator] for uber jars.
///
/// Depending on the request, the generator provides two types of resources.
/// 
/// 1. A [JarFile]. This type of resource is also returned if a more
///    general [ResourceType] such as [ClasspathElement] is requested.
///
/// 2. An [AppJarFile]. When requesting this special jar type, the
///    generator checks if a main class is specified.
///
/// The generator takes the following approach:
/// 
///   * Request `Resources<ClasspathElement>` from the providers. Add the
///     resource trees and the jar files to the sources to be processed.
///     Ignore jar files from maven repositories (instances of
///     [MvnRepoJarFile]).
///   * Request all [MvnRepoResource]s from the providers and use them for
///     a dependency resolution. Add the jar files from the dependency
///     resolution to the resources to be processed.
///   * Add resources from the sources to the uber jar. Merge the files in
///     `META-INF/services/` that have the same name by concatenating them.
///   * Filter out any other duplicate direct child files of `META-INF`.
///     These files often contain information related to the origin jar
///     that is not applicable to the uber jar.
///   * Filter out any module-info.class entries.
///
/// Note that the [UberJarGenerator] does deliberately not request the
/// [ClasspathElement]s as `RuntimeResources` because this may return
/// resources twice if a project uses another project as runtime
/// dependency (i.e. with [Intent#Consume]. If this rule causes entries
/// to be missing, simply add them explicitly.  
/// 
/// The resource type of the uber jar generator's output is one
/// of the resource types of its inputs, because uber jars can also be used
/// as [ClasspathElement]. Therefore, if you want to create an uber jar
/// from all resources provided by a project, you must not add the
/// generator to the project like this:
/// ```java
///     generator(UberJarGenerator::new).add(this); // Circular dependency
/// ```
///
/// This would add the project as provider and thus make the uber jar
/// generator as supplier to the project its own provider (via
/// [Project.resources][Project#resources]). Rather, you have to use this
/// slightly more complicated approach to adding providers to the uber
/// jar generator:
/// ```java
///     generator(UberJarGenerator::new)
///         .addAll(providers().select(Forward, Expose, Supply));
/// ```
/// This requests the same providers from the project as 
/// [Project.resources][Project#resources] does, but allows the uber jar
/// generator's [addFrom] method to filter out the uber jar
/// generator itself from the providers. The given intents can
/// vary depending on the requirements.
///
/// If you don't want the generated uber jar to be available to other
/// generators of your project, you can also add it to a project like this:
/// ```java
///     dependency(new UberJarGenerator(this)
///         .from(providers(EnumSet.of(Forward, Expose, Supply))), Intent.Forward)
/// ```
///
/// Of course, the easiest thing to do is separate the generation of
/// class trees or library jars from the generation of the uber jar by
/// generating the uber jar in a project of its own. Often the root
/// project can be used for this purpose.  
///
public class UberJarGenerator extends LibraryGenerator {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private Map<Path, java.util.jar.JarFile> openJars = Map.of();
    private Predicate<Resource> resourceFilter = _ -> true;

    /// Instantiates a new uber jar generator.
    ///
    /// @param project the project
    ///
    public UberJarGenerator(Project project) {
        super(project);
    }

    @Override
    public UberJarGenerator name(String name) {
        rename(name);
        return this;
    }

    @Override
    protected void
            collectFromProviders(Map<Path, Resources<IOResource>> contents) {
        openJars = new ConcurrentHashMap<>();
        providers().stream().map(p -> p.resources(
            of(ClasspathElementType).using(Supply, Expose)))
            .flatMap(s -> s).parallel()
            .filter(resourceFilter::test).forEach(cpe -> {
                if (cpe instanceof FileTree<?> fileTree) {
                    collect(contents, fileTree);
                } else if (cpe instanceof JarFile jarFile
                    // Ignore jar files from maven repositories, see below
                    && !(jarFile instanceof MvnRepoJarFile)) {
                    addJarFile(contents, jarFile, openJars);
                }
            });

        // Jar files from maven repositories must be resolved before
        // they can be added to the uber jar, i.e. they must be added
        // with their transitive dependencies.
        var lookup = new MvnRepoLookup();
        lookup.resolve(providers().stream().map(
            p -> p.resources(of(MvnRepoDependencyType).usingAll()))
            .flatMap(s -> s));
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
    public UberJarGenerator resourceFilter(Predicate<Resource> filter) {
        resourceFilter = filter;
        return this;
    }

    private void addJarFile(Map<Path, Resources<IOResource>> entries,
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
                    _ -> project().newResource(IOResourcesType))
                    .add(new JarFileEntry(jar, e));
            });
    }

    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition",
        "PMD.PreserveStackTrace", "PMD.UselessPureMethodCall" })
    @Override
    protected void resolveDuplicates(Map<Path, Resources<IOResource>> entries) {
        entries.entrySet().parallelStream().forEach(item -> {
            var candidates = item.getValue();
            if (candidates.stream().count() == 1) {
                return;
            }
            var entryName = item.getKey();
            if (entryName.startsWith("META-INF/services")) {
                var combined = new ServicesEntryResource();
                candidates.stream().forEach(service -> {
                    try {
                        combined.add(service);
                    } catch (IOException e) {
                        throw new BuildException("Cannot read " + service);
                    }
                });
                candidates.clear();
                candidates.add(combined);
                return;
            }
            if (entryName.startsWith("META-INF")) {
                candidates.clear();
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
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.accepts(AppJarFileType)
            && !requested.accepts(CleanlinessType)) {
            return Stream.empty();
        }

        // Maybe only delete
        if (requested.accepts(CleanlinessType)) {
            destination().resolve(jarName()).toFile().delete();
            return Stream.empty();
        }

        // Make sure mainClass is set for app jar
        if (requested.requires(AppJarFileType) && mainClass() == null) {
            throw new BuildException("Main class must be set for "
                + name() + " in " + project());
        }

        // Upgrade to most specific type to avoid duplicate generation
        if (mainClass() != null && !requested.type().equals(AppJarFileType)) {
            return (Stream<T>) context()
                .resources(this, project().of(AppJarFileType));
        }
        if (mainClass() == null && !requested.type().equals(JarFileType)) {
            return (Stream<T>) context()
                .resources(this, project().of(JarFileType));
        }

        // Prepare jar file
        var destDir = destination();
        if (!destDir.toFile().exists()) {
            if (!destDir.toFile().mkdirs()) {
                throw new BuildException("Cannot create directory " + destDir);
            }
        }
        var jarResource = requested.requires(AppJarFileType)
            ? project().newResource(AppJarFileType,
                destDir.resolve(jarName()))
            : project().newResource(LibraryJarFileType,
                destDir.resolve(jarName()));
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
        return Stream.of((T) jarResource);
    }
}
