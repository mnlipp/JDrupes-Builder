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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.InputResource;
import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceRetriever;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.core.StreamCollector;
import static org.jdrupes.builder.java.JavaTypes.*;

/// A generator that creates a `module-info.java` descriptor from the
/// project's sources, dependencies, and service entries.
///
/// The generated module descriptor is written to a build directory and
/// provided as a [FileTree] of [JavaSourceFile]s so that the [JavaCompiler]
/// picks it up alongside the regular sources.
///
/// ## Module name
///
/// The module name defaults to the project name sanitized for JPMS
/// requirements (lowercased, dots replaced with underscores). Override
/// via [moduleName].
///
/// ## Exports
///
/// All packages found in the configured source trees are exported.
///
/// ## Requires
///
/// Dependencies that are modular JARs (containing `module-info.class`)
/// are added as `requires` directives. The module name is extracted
/// from the module descriptor.
///
/// ## Provides / With
///
/// `META-INF/services/` entries in the project's resource trees are
/// converted to `provides ... with ...` directives.
///
@SuppressWarnings("PMD.GodClass")
public class ModuleDescriptorGenerator
        extends AbstractGenerator implements ResourceRetriever {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Pattern packagePattern
        = Pattern.compile("^\\s*package\\s+(\\S+)\\s*;",
            Pattern.MULTILINE);
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final Pattern servicesPattern
        = Pattern.compile("^META-INF/services/(.+)$");

    private final StreamCollector<FileTree<JavaSourceFile>> sources
        = StreamCollector.cached();
    private final StreamCollector<FileTree<?>> resourceTrees
        = StreamCollector.cached();
    private final StreamCollector<ResourceProvider> providers
        = StreamCollector.cached();
    private String moduleName;
    private Path destination = Path.of("generated/module-info/java");

    /// Instantiates a new module descriptor generator.
    ///
    /// @param project the project
    ///
    public ModuleDescriptorGenerator(Project project) {
        super(project);
    }

    @Override
    public ModuleDescriptorGenerator name(String name) {
        rename(name);
        return this;
    }

    /// Returns the configured module name.
    ///
    /// @return the module name
    ///
    public String moduleName() {
        return moduleName;
    }

    /// Sets the module name. If not set, the project name is sanitized
    /// for JPMS requirements.
    ///
    /// @param moduleName the module name
    /// @return this generator
    ///
    public ModuleDescriptorGenerator moduleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    /// Returns the destination directory for the generated module
    /// descriptor. Defaults to `generated/module-info/java` under
    /// the project's build directory.
    ///
    /// @return the destination
    ///
    public Path destination() {
        return destination;
    }

    /// Sets the destination directory. The [Path] is resolved against
    /// the project's build directory.
    ///
    /// @param destination the new destination
    /// @return this generator
    ///
    public ModuleDescriptorGenerator destination(Path destination) {
        this.destination = destination;
        return this;
    }

    /// Adds source trees to scan for package declarations.
    ///
    /// @param sources the sources
    /// @return this generator
    ///
    public ModuleDescriptorGenerator
            addSources(Stream<FileTree<JavaSourceFile>> sources) {
        this.sources.add(sources);
        return this;
    }

    /// Adds resource trees to scan for `META-INF/services/` entries.
    ///
    /// @param trees the resource trees
    /// @return this generator
    ///
    public ModuleDescriptorGenerator
            addResources(Stream<FileTree<?>> trees) {
        resourceTrees.add(trees);
        return this;
    }

    @Override
    public ModuleDescriptorGenerator addFrom(ResourceProvider... providers) {
        addFrom(Stream.of(providers));
        return this;
    }

    @Override
    public ModuleDescriptorGenerator
            addFrom(Stream<ResourceProvider> providers) {
        this.providers.add(providers);
        return this;
    }

    @Override
    protected <T extends Resource> Collection<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.accepts(JavaSourceTreeType)
            && !requested.accepts(CleanlinessType)) {
            return Collections.emptyList();
        }

        var destDir = project().buildDirectory().resolve(destination);
        var generatedTree = FileTree.of(
            project(), destDir, JavaSourceFile.class, "**/*.java");

        if (requested.accepts(CleanlinessType)) {
            generatedTree.cleanup();
            return Collections.emptyList();
        }

        // Upgrade to most specific type
        if (!requested.type().equals(JavaSourceTreeType)) {
            @SuppressWarnings("unchecked")
            var result
                = (Collection<T>) resources(of(JavaSourceTreeType)).toList();
            return result;
        }

        if (needsRegeneration(generatedTree)) {
            generateDescriptor(destDir);
        }

        @SuppressWarnings("unchecked")
        var result = (Collection<T>) List.of(generatedTree);
        return result;
    }

    /// Checks whether the generated descriptor needs to be regenerated.
    ///
    /// @param tree the generated source tree
    /// @return true if regeneration is needed
    ///
    private boolean needsRegeneration(FileTree<JavaSourceFile> tree) {
        var descriptorPath = tree.root().resolve("module-info.java");
        if (!Files.exists(descriptorPath)) {
            return true;
        }

        for (var sourceTree : sources.stream().toList()) {
            if (sourceTree.isNewerThan(FileResource.of(descriptorPath))) {
                return true;
            }
        }
        for (var resourceTree : resourceTrees.stream().toList()) {
            if (resourceTree.isNewerThan(FileResource.of(descriptorPath))) {
                return true;
            }
        }
        return false;
    }

    /// Generates the `module-info.java` file.
    ///
    /// @param destDir the destination directory
    ///
    @SuppressWarnings({ "PMD.AvoidCatchingGenericException", "PMD.NcssCount" })
    private void generateDescriptor(Path destDir) {
        var name = resolveModuleName();
        var packages = collectPackages();
        var requires = collectRequires();
        var services = collectServices();

        if (packages.isEmpty() && requires.isEmpty()
            && services.isEmpty()) {
            logger.atInfo().log("No content for module descriptor of %s, "
                + "skipping generation.", project().name());
            return;
        }

        logger.atInfo().log("Generating module descriptor %s", name);
        try {
            Files.createDirectories(destDir);
            var descriptorPath = destDir.resolve("module-info.java");
            try (var writer = Files.newBufferedWriter(
                descriptorPath, StandardCharsets.UTF_8)) {
                writer.write("open module ");
                writer.write(name);
                writer.write(" {\n");

                // Requires directives
                for (var req : requires) {
                    writer.write("    requires ");
                    writer.write(req);
                    writer.write(";\n");
                }
                if (!requires.isEmpty()) {
                    writer.write("\n");
                }

                // Exports directives
                for (var pkg : packages) {
                    writer.write("    exports ");
                    writer.write(pkg);
                    writer.write(";\n");
                }
                if (!packages.isEmpty()) {
                    writer.write("\n");
                }

                // Provides / with directives
                for (var entry : services.entrySet()) {
                    writer.write("    provides ");
                    writer.write(entry.getKey());
                    writer.write(" with ");
                    writer.write(String.join(", ", entry.getValue()));
                    writer.write(";\n");
                }

                writer.write("}\n");
            }
            logger.atFine().log("Module descriptor written to %s",
                descriptorPath);
        } catch (Exception e) {
            logger.atSevere().withCause(e)
                .log("Project %s: Cannot generate module descriptor",
                    project().name());
            throw new BuildException().from(this).cause(e);
        }
    }

    /// Resolves the module name from configuration or project name.
    ///
    /// @return the module name
    ///
    private String resolveModuleName() {
        if (moduleName != null) {
            return moduleName;
        }
        return project().name().replace('-', '.')
            .replaceAll("[^\\p{L}\\p{N}_.]", "_");
    }

    /// Collects package declarations from source trees.
    ///
    /// @return the set of packages
    ///
    private Set<String> collectPackages() {
        var packages = new TreeSet<String>();
        for (var sourceTree : sources.stream().toList()) {
            for (var file : sourceTree.get()) {
                var path = file.path();
                if (path.endsWith("module-info.java")) {
                    continue;
                }
                try {
                    var content = Files.readString(path);
                    Matcher matcher = packagePattern.matcher(content);
                    if (matcher.find()) {
                        packages.add(matcher.group(1));
                    }
                } catch (IOException e) {
                    logger.atWarning().withCause(e)
                        .log("Cannot read %s", path);
                }
            }
        }
        return packages;
    }

    /// Collects module names from modular dependencies.
    ///
    /// @return the set of required module names
    ///
    private Set<String> collectRequires() {
        var requires = new LinkedHashSet<String>();
        var cpResources = Resources.of(CodeContributionsType).addAll(
            project().providers(Consume, Reveal, Expose)
                .resources(of(CodeContributionType)));

        for (var element : cpResources.get()) {
            if (element.isModular()) {
                var modName = extractModuleNameFromJar(element);
                modName.ifPresent(requires::add);
            }
        }
        return requires;
    }

    /// Extracts the module name from a JAR or directory.
    ///
    /// @param element the classpath element
    /// @return the optional module name
    ///
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private Optional<String>
            extractModuleNameFromJar(CodeContribution element) {
        try {
            var finder = java.lang.module.ModuleFinder.of(element.toPath());
            return finder.findAll().stream()
                .map(ref -> ref.descriptor().name())
                .findFirst();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /// Collects `provides/with` directives from service files.
    ///
    /// @return the service interface to implementation classes mapping
    ///
    private Map<String, Set<String>> collectServices() {
        var services = new LinkedHashMap<String, Set<String>>();

        for (var tree : resourceTrees.stream().toList()) {
            for (var entry : tree.get()) {
                var relPath = tree.root().relativize(entry.path())
                    .toString().replace('\\', '/');
                Matcher matcher = servicesPattern.matcher(relPath);
                if (matcher.find()) {
                    var serviceName = matcher.group(1);
                    var implClasses = collectImplementations(entry);
                    services.merge(serviceName, implClasses,
                        (existing, added) -> {
                            existing.addAll(added);
                            return existing;
                        });
                }
            }
        }
        return services;
    }

    /// Reads the implementations listed in a service file.
    ///
    /// @param resource the service file
    /// @return the set of implementation class names
    ///
    private Set<String> collectImplementations(InputResource resource) {
        var classes = new TreeSet<String>();
        try (var stream = resource.inputStream()) {
            var content = new String(stream.readAllBytes(),
                StandardCharsets.UTF_8);
            content.lines()
                .filter(l -> !l.isBlank())
                .filter(l -> !l.startsWith("//"))
                .map(String::trim)
                .forEach(classes::add);
        } catch (IOException e) {
            logger.atWarning().withCause(e).log("Cannot read service file");
        }
        return classes;
    }
}
