/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
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

package org.jdrupes.builder.bnd;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.version.Version;
import com.google.common.flogger.FluentLogger;
import static com.google.common.flogger.LazyArgs.lazy;
import io.vavr.control.Try;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.ConfigurationException;
import org.jdrupes.builder.api.Generator;
import static org.jdrupes.builder.api.Intent.Consume;
import static org.jdrupes.builder.api.Intent.Expose;
import static org.jdrupes.builder.api.Intent.Reveal;
import static org.jdrupes.builder.api.Intent.Supply;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.java.ClassTree;
import org.jdrupes.builder.java.JavaCompiler;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.LibraryJarFile;
import org.jdrupes.builder.java.ManifestAttributes;

/// A [Generator] that computes OSGi metadata in response to requests for
/// [ManifestAttributes].
///
/// This implementation uses the `bndlib` library from the
/// [bnd](https://github.com/bndtools/bnd) project to analyze bundle
/// contents and compute manifest attributes.
///
/// When invoked, the analyzer first obtains resources of type [ClassTree]
/// supplied to the project (typically by a [JavaCompiler]). These class
/// trees are treated as the content of the bundle.
///
/// It then obtains resources of type [LibraryJarFile] from the project's
/// dependencies with intents `Consume`, `Reveal` and `Expose` (the same
/// intents as used by the [JavaCompiler] when assembling the compilation
/// classpath). These library resources are registered as bundle
/// dependencies.
///
/// The collected class tree and library resources are analyzed by `bndlib`
/// to produce the manifest attributes requested.
///
/// Contrary to most [ResourceProvider]s, the [BndAnalyzer] needs project
/// specific informations (supplied as instructions). This can be handled
/// in multiple ways. One approach is to add the [BndAnalyzer] with the
/// instructions in the project’s constructor rather than in 
/// [RootProject#prepareProject]. Alternatively, put project-specific
/// instructions in a `bnd.bnd` file in the project's directory, then
/// register the analyzer in [RootProject#prepareProject] and add the
/// instructions via [#instructions(Path)], where `Path` refers to the
/// `bnd.bnd` file.
///
@SuppressWarnings("PMD.TooManyStaticImports")
public class BndAnalyzer extends AbstractBndGenerator {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    /// Initializes a new osgi analyzer.
    ///
    /// @param project the project
    ///
    public BndAnalyzer(Project project) {
        super(project);
    }

    /// Add the instruction specified by key and value.
    ///
    /// @param key the key
    /// @param value the value
    /// @return the bnd analyzer
    ///
    @Override
    public BndAnalyzer instruction(String key, String value) {
        super.instruction(key, value);
        return this;
    }

    /// Add the given instructions for the analyzer.
    ///
    /// @param instructions the instructions
    /// @return the bnd analyzer
    ///
    @Override
    public BndAnalyzer instructions(Map<String, String> instructions) {
        super.instructions(instructions);
        return this;
    }

    /// Add the instructions from the given bnd (properties) file.
    ///
    /// @param bndFile the bnd file
    /// @return the bnd analyzer
    ///
    @Override
    public BndAnalyzer instructions(Path bndFile) {
        super.instructions(bndFile);
        return this;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.accepts(ManifestAttributesType)) {
            return Stream.empty();
        }
        try (var analyzer = new Analyzer();
                var jar = new aQute.bnd.osgi.Jar("dot")) {
            // Assemble bundle content
            var content = newResource(ClassTreesType).addAll(project()
                .providers().resources(of(ClassTree.class).using(Supply)));
            // A bnd ("better never document") Jar can actually be a
            // classfile tree, and several such "Jar"s can be merged.
            // IOException will be throw (.get()) and handled in the outer try
            vavrStream(content).find(_ -> true).peek(t -> Try.of(() -> jar
                .addAll(new aQute.bnd.osgi.Jar(t.root().toFile()))).get());
            analyzer.setJar(jar);
            applyInstructions(analyzer);

            // Add classpath dependencies
            var bundleDeps = newResource(
                new ResourceType<Resources<LibraryJarFile>>() {}).addAll(
                    project().providers(Consume, Reveal, Expose)
                        .resources(project().of(LibraryJarFileType)));
            logger.atFiner().log("BndAnalyzer in"
                + " %s uses dependencies %s", project(),
                lazy(() -> bundleDeps.stream().map(e -> e.path().toString())
                    .collect(Collectors.joining(File.pathSeparator))));
            // IOException will be throw (.get()) and handled in the outer try
            vavrStream(bundleDeps).forEach(dep -> Try
                .run(() -> analyzer.addClasspath(dep.path().toFile())).get());

            // Evaluate and convert to result type
            var manifest = analyzer.calcManifest();
            verifyManifest(manifest);
            var asResource = newResource(ManifestAttributesType);
            asResource.putAll(manifest.getMainAttributes());
            @SuppressWarnings("unchecked")
            var result = (T) asResource;
            return Stream.of(result);
        } catch (Exception e) {
            throw new BuildException().from(this).cause(e);
        }
    }

    private void verifyManifest(Manifest manifest) {
        Optional.ofNullable((String) manifest.getMainAttributes()
            .get(new Attributes.Name("Bundle-Version"))).ifPresent(v -> {
                try {
                    new Version(v);
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException().message(
                        "Attempt to specify invalid OSGi version %s", v)
                        .from(this).cause(e);
                }
            });

    }
}
