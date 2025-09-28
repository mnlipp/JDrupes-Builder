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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.IOResource;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.java.AppJarFile;
import org.jdrupes.builder.java.ClasspathElement;
import org.jdrupes.builder.java.JarFile;
import org.jdrupes.builder.java.JarFileEntry;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.LibraryJarGenerator;
import org.jdrupes.builder.java.RuntimeResources;
import org.jdrupes.builder.java.ServicesEntryResource;
import org.jdrupes.builder.mvnrepo.MvnRepoJarFile;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

/// A [Generator] for uber jars.
///
/// Depending on the request, the generator provides two types of resources.
/// 
/// 1. A [JarFile]. This type of resource is also returned if a more
///    general [ResourceType] such as [ClasspathElement] is requested.
///
/// 2. An [AppJarFile].
///
/// The generator takes a simple approach:
/// 
///   * Add the content of the [ClasspathElement]s added with [add][#add]
///     or [addAll][#addAll] to the resulting uber jar.
///   * Filter out any direct child files of `META-INF`. These files often
///     contain information related to the origin jar that is not applicable
///     to the uber jar.
///   * Merge the files in `META-INF/services/` that have the same name by
///     concatenating them.
///
/// Note that the resource type of the uber jar generator's output is one
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
public class UberJarGenerator extends LibraryJarGenerator {

    private Map<Path, java.util.jar.JarFile> openJars = Map.of();

    /// Instantiates a new uber jar generator.
    ///
    /// @param project the project
    ///
    public UberJarGenerator(Project project) {
        super(project);
    }

    @Override
    @SuppressWarnings("PMD.UseDiamondOperator")
    protected void
            collectFromProviders(Map<Path, Resources<IOResource>> contents) {
        openJars = new ConcurrentHashMap<>();
        project().getFrom(providers().stream(),
            new ResourceRequest<ClasspathElement>(
                new ResourceType<RuntimeResources>() {}))
            .forEach(cpe -> {
                if (cpe instanceof FileTree<?> fileTree) {
                    addFileTree(contents, fileTree);
                } else if (cpe instanceof JarFile jarFile
                    && !(jarFile instanceof MvnRepoJarFile)) {
                    addJarFile(contents, jarFile, openJars);
                }
            });
        var lookup = new MvnRepoLookup(project());
        project().getFrom(providers().stream(),
            new ResourceRequest<>(MvnRepoDependenciesType))
            .forEach(d -> lookup.artifact(d.toString()));
        project().context().get(lookup, new ResourceRequest<ClasspathElement>(
            new ResourceType<RuntimeResources>() {}))
            .forEach(cpe -> {
                if (cpe instanceof MvnRepoJarFile jarFile) {
                    addJarFile(contents, jarFile, openJars);
                }
            });
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
                    _ -> project().newResource(IOResourcesType))
                    .add(new JarFileEntry(jar, e));
            });
    }

    @Override
    @SuppressWarnings({ "PMD.CollapsibleIfStatements", "unchecked",
        "PMD.CloseResource", "PMD.UseTryWithResources" })
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.includes(AppJarFileType)
            && !requested.includes(Cleaniness)) {
            return Stream.empty();
        }

        // Prepare jar file
        var destDir = Optional.ofNullable(destination())
            .orElseGet(() -> project().buildDirectory().resolve("libs"));
        if (!destDir.toFile().exists()) {
            if (!destDir.toFile().mkdirs()) {
                throw new BuildException("Cannot create directory " + destDir);
            }
        }

        // Maybe only delete
        if (requested.includes(Cleaniness)) {
            project().newResource(JarFileType,
                destDir.resolve(jarName())).delete();
            return Stream.empty();
        }

        // Make sure mainClass is set for app jar
        if (AppJarFileType.isAssignableFrom(requested.type().containedType())
            && mainClass() == null) {
            throw new BuildException("Main class must be set for "
                + name() + " in " + project());
        }

        // Add main class if defined
        if (mainClass() != null) {
            attributes(
                Map.of(Attributes.Name.MAIN_CLASS, mainClass()).entrySet()
                    .stream());
        }

        // Narrow too general requests
        var requestedResource = requested.type().containedType();
        if (!JarFile.class.isAssignableFrom(requestedResource.getClass())) {
            requestedResource = JarFileType;
        }

        var jarResource = (JarFile) project().newResource(requestedResource,
            destDir.resolve(jarName()));
        try {
            buildJar(jarResource);
        } finally {
            // buidJar indirectly calls collectFromProviders which opens
            // resources that are used in builJar. Close them now.
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
                log.warning(() -> "Entry " + entryName + " from " + a
                    + " duplicates entry from " + b + " and is skipped.");
                return a;
            });
        });
    }
}
