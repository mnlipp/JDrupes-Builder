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

package org.jdrupes.builder.core;

import com.google.common.flogger.FluentLogger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceRequest;
import static org.jdrupes.builder.api.ResourceType.CleanlinessType;

/// A provider that generates a [FileTree] from existing file trees.
/// In general, copying file trees should be avoided. However, in some
/// situations a resource provider and a consumer cannot be configured
/// so that the output of the former can be used directly by the latter.
/// 
/// The provider generates a [FileTree] in the directory specified 
/// with [#into] by copying files from the sources defined with one
/// of the `source`-methods. The class is not named `Copier`
/// because the specification of [Source]s supports transformations
/// beyond simply copying.   
/// 
/// The provider generates the [FileTree] in response to a request that
/// matches the one set with [#requestForResult]. The content of the
/// generated file tree is returned using the type specified in the
/// request.
/// 
/// A request for [Cleanliness] deletes the directory specified with
/// [#into].
///
public class FileTreeBuilder extends AbstractGenerator {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final StreamCollector<Source> sources = new StreamCollector<>(true);
    private Path destination;
    private ResourceRequest<?> requestForResult;

    /// Describes a source that contributes files to the generated
    /// tree.
    ///
    public static final class Source {
        private final FileTree<?> tree;
        private Function<Path, Path> rename;
        private BiConsumer<InputStream, OutputStream> filter;
        private BiConsumer<BufferedReader, PrintStream> textFilter;
        private Charset charset;

        private Source(FileTree<?> tree) {
            this.tree = tree;
        }

        /// Creates a new source specification.
        /// 
        /// @param tree the source file tree
        /// @return the source
        ///
        @SuppressWarnings("PMD.ShortMethodName")
        public static Source of(FileTree<?> tree) {
            return new Source(tree);
        }

        /// Specifies a function for renaming source files. The receives
        /// the file's path relative to the source tree's root. It must
        /// return the file's path relative to the destination set with
        /// [#into].
        ///
        /// @param renamer the function for renaming
        /// @return the source
        ///
        public Source rename(Function<Path, Path> renamer) {
            this.rename = renamer;
            return this;
        }

        /// Specifies a function for copying the content of the source
        /// file to the destination file. If set, this function
        /// is invoked for each file instead of simply copying the file
        /// content.
        ///
        /// @param filter the copy function
        /// @return the source
        ///
        public Source filter(BiConsumer<InputStream, OutputStream> filter) {
            this.filter = filter;
            return this;
        }

        /// Specifies a function for copying the content of the source
        /// text file to the destination text file. If set, this function
        /// is invoked for each file instead of simply copying the file
        /// content.
        ///
        /// @param filter the copy function
        /// @param charset the charset
        /// @return the source
        ///
        public Source filter(BiConsumer<BufferedReader, PrintStream> filter,
                Charset charset) {
            this.textFilter = filter;
            this.charset = charset;
            return this;
        }
    }

    /// Initializes a new file tree builder.
    ///
    /// @param project the project
    ///
    public FileTreeBuilder(Project project) {
        super(project);
    }

    /// Adds the given [Stream] of [Source] specifications.
    ///
    /// @param sources the sources
    /// @return the file tree builder
    ///
    public FileTreeBuilder source(Stream<Source> sources) {
        this.sources.add(sources);
        return this;
    }

    /// Convenience method for adding a [Source] without renaming or
    /// filter to the sources. If `root` is a relative path, it is resolved
    /// against the project's directory.
    ///
    /// @param root the root
    /// @param pattern the pattern
    /// @return the file tree builder
    ///
    public FileTreeBuilder source(Path root, String pattern) {
        sources.add(Stream.of(Source.of(FileTree.of(
            project(), root, pattern))));
        return this;
    }

    /// Convenience method for adding a [Source] with optional renaming and
    /// filtering to the sources. If `root` is a relative path, it is
    /// resolved against the project's directory.
    ///
    /// @param root the root
    /// @param pattern the pattern
    /// @param renamer the renamer (may be `null`)
    /// @param filter the filter (may be `null`)
    /// @return the file tree builder
    ///
    public FileTreeBuilder source(Path root, String pattern,
            Function<Path, Path> renamer,
            BiConsumer<InputStream, OutputStream> filter) {
        var source = Source.of(FileTree.of(project(), root, pattern));
        if (renamer != null) {
            source.rename(renamer);
        }
        if (filter != null) {
            source.filter(filter);
        }
        sources.add(Stream.of(source));
        return this;
    }

    /// Sets the destination directory for the generated file tree. If the
    /// destination is relative, it is resolved against the project's
    /// directory.
    ///
    /// @param destination the destination
    /// @return the file tree builder
    ///
    public FileTreeBuilder into(Path destination) {
        if (!destination.isAbsolute()) {
            destination = project().directory().resolve(destination);
        }
        if (destination.toFile().exists()
            && !destination.toFile().isDirectory()) {
            throw new IllegalArgumentException(
                "Destination path \"" + destination
                    + "\" exists but is not a directory.");
        }
        this.destination = destination.normalize();
        return this;
    }

    /// Configures the request that this builder responds to by
    /// providing the generated file tree.
    ///
    /// @param proto a prototype request describing the requests that
    /// the provider should respond to
    /// @return the file tree builder
    ///
    public FileTreeBuilder provideResources(
            ResourceRequest<? extends FileTree<?>> proto) {
        requestForResult = proto;
        return this;
    }

    @Override
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> request) {
        if (request.accepts(CleanlinessType)) {
            FileTree.of(project(), destination, "**/*").cleanup();
            return Stream.empty();
        }

        // Check if request matches
        if (requestForResult == null
            || !request.accepts(requestForResult.type())
            || (!requestForResult.name().isEmpty()
                && !Objects.equals(requestForResult.name().get(),
                    request.name().orElse(null)))) {
            return Stream.empty();
        }

        // Always evaluate for most special type
        if (!request.equals(requestForResult)) {
            @SuppressWarnings({ "unchecked" })
            var result = (Stream<T>) resources(requestForResult);
            return result;
        }

        if (destination == null) {
            throw new IllegalStateException("No destination set.");
        }

        // Retrieve the sources
        var required = sources.stream().toList();
        if (!createInDestination(required)) {
            logger.atFine().log("Output from %s is up to date", this);
        }

        var result = ResourceFactory.create(request.type(), project(),
            destination, "**/*");
        return Stream.of(result);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private boolean createInDestination(List<Source> required) {
        boolean changed = false;
        for (var source : required) {
            var srcTree = source.tree;
            changed |= srcTree.entries().parallel().map(entry -> {
                try {
                    return createTarget(source, entry);
                } catch (IOException e) {
                    throw new BuildException().from(this).cause(e);
                }
            }).reduce(false, (a, b) -> a || b);
        }
        return changed;
    }

    private boolean createTarget(Source source, Path entry) throws IOException {
        var src = source.tree.root().resolve(entry);
        var dest = destination.resolve(entry);
        var rename = source.rename;
        if (rename != null) {
            dest = destination.resolve(rename.apply(entry));
            if (!dest.normalize().startsWith(destination)) {
                throw new BuildException().from(this).message(
                    "Rename function returns \"%s\" which is outside the"
                        + " target directory \"%s\"",
                    dest, destination);
            }
        }
        if (src.toFile().isDirectory()) {
            if (!src.toFile().exists()) {
                Files.createDirectories(dest);
                return true;
            }
            return false;
        }
        Files.createDirectories(dest.getParent());
        if (dest.toFile().exists() && dest.toFile()
            .lastModified() >= src.toFile().lastModified()) {
            return false;
        }
        if (source.filter != null) {
            try (var srcStream = Files.newInputStream(src);
                    var destStream = Files.newOutputStream(dest)) {
                source.filter.accept(srcStream, destStream);
            }
            return true;
        }
        if (source.textFilter != null) {
            try (var reader = Files.newBufferedReader(src, source.charset);
                    var out = new PrintStream(dest.toFile(), source.charset)) {
                source.textFilter.accept(reader, out);
            }
            return true;
        }
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

}
