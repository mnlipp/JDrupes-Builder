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
import com.google.common.flogger.FluentLogger;
import static com.google.common.flogger.LazyArgs.lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
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
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.java.ClassTree;
import org.jdrupes.builder.java.JavaCompiler;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.LibraryJarFile;
import org.jdrupes.builder.java.ManifestAttributes;

/// A provider that computes OSGi attributes in response to
/// requests for [ManifestAttributes]. It uses the `bndlib`
/// from the [bnd](https://github.com/bndtools/bnd) project
/// for its implementation.
/// 
/// The analyzer first requests the resources of type [ClassTree] supplied
/// to the project (typically by its [JavaCompiler]). The classes obtained
/// are assumed to be the content of the bundle.
/// 
/// It then requests the resources of type [LibraryJarFile] from the
/// project's dependencies with intent `Consume` and `Expose` (same
/// intents as used by the [JavaCompiler] when assembling the compilation
/// classpath). These libraries are registered as the dependencies of the
/// bundle.
/// 
/// The collected is analyzed and used to provide the requested attributes.
/// 
/// The [BndAnalyzer] can be configured for a [Project] in various ways.
/// Contrary to most [ResourceProvider]s, it needs a lot of sub project
/// specific information (supplied as instructions). One way to handle
/// this is to add the [BndAnalyzer] to the project in the project's
/// constructor instead of in [RootProject#prepareProject].
/// 
/// Another way is to put the project specific instructions in a file
/// `bnd.bnd` (common name) in the project's directory. Then add the
/// [BndAnalyzer] in [RootProject#prepareProject] with an invocation of
/// [#instructions(Path)], where path refers the project's `bnd.bnd`.
///
@SuppressWarnings("PMD.TooManyStaticImports")
public class BndAnalyzer extends AbstractGenerator {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final List<Tuple2<String, String>> instructions = new ArrayList<>();

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
    public BndAnalyzer instruction(String key, String value) {
        instructions.add(Tuple.of(key, value));
        return this;
    }

    /// Add the given instructions for the analyzer.
    ///
    /// @param instructions the instructions
    /// @return the bnd analyzer
    ///
    public BndAnalyzer instructions(Map<String, String> instructions) {
        instructions.forEach(this::instruction);
        return this;
    }

    /// Add the instructions from the given bnd (properties) file.
    ///
    /// @param bndFile the bnd file
    /// @return the bnd analyzer
    ///
    public BndAnalyzer instructions(Path bndFile) {
        var props = new Properties();
        try {
            props.load(Files.newInputStream(bndFile));
            props.forEach((k, v) -> instruction(k.toString(), v.toString()));
        } catch (IOException e) {
            throw new BuildException("Cannot read bnd file " + bndFile, e);
        }
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
            instructions.forEach(t -> analyzer.setProperty(t._1, t._2));

            // Add classpath dependencies
            var bundleDeps = newResource(
                new ResourceType<Resources<LibraryJarFile>>() {}).addAll(
                    project().providers(Consume, Reveal, Expose)
                        .resources(project().of(LibraryJarFileType)));
            logger.atFiner().log("Bnd in %s uses dependencies %s", project(),
                lazy(() -> bundleDeps.stream().map(e -> e.path().toString())
                    .collect(Collectors.joining(File.pathSeparator))));
            // IOException will be throw (.get()) and handled in the outer try
            vavrStream(bundleDeps).forEach(dep -> Try
                .run(() -> analyzer.addClasspath(dep.path().toFile())).get());

            // Evaluate and convert to result type
            var manifest = analyzer.calcManifest();
            var asResource = newResource(ManifestAttributesType);
            asResource.putAll(manifest.getMainAttributes());
            @SuppressWarnings("unchecked")
            var result = (T) asResource;
            return Stream.of(result);
        } catch (Exception e) {
            throw new BuildException("Failed to generate OSGi metadata", e);
        }
    }
}
