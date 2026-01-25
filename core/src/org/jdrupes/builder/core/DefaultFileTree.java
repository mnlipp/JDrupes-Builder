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

package org.jdrupes.builder.core;

import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Proxyable;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;

/// The default implementation of a [FileTree].
///
/// @param <T> the type of the [FileResource]s in the tree.
///
public class DefaultFileTree<T extends FileResource> extends DefaultResources<T>
        implements FileTree<T> {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private Instant latestChange = Instant.MIN;
    private final Project project;
    private final Path root;
    private final String pattern;
    private final List<String> excludes = new ArrayList<>();
    private boolean withDirs;
    private boolean filled;

    /// Returns a new file tree. The file tree includes all files
    /// matching `pattern` in the tree starting at `root`. `root`
    /// may be specified as absolute path or as path relative to the
    /// `project`'s directory (see [Project#directory]).
    /// 
    /// if `project` is `null`, and `root` is a relative path,
    /// `root` is resolved against the current working directory.
    /// `pattern`
    ///
    /// @param type the resource type
    /// @param project the project
    /// @param root the root
    /// @param pattern the pattern
    ///
    protected DefaultFileTree(ResourceType<?> type, Project project, Path root,
            String pattern) {
        super(type);
        this.project = project;
        this.root = root;
        this.pattern = pattern;
    }

    /// Creates the a new [FileTree].
    ///
    /// @param <T> the tree's type
    /// @param type the type
    /// @param project the project
    /// @param root the root
    /// @param pattern the pattern
    /// @return the file tree
    ///
    @SuppressWarnings("unchecked")
    public static <T extends FileTree<?>>
            T createFileTree(ResourceType<T> type, Project project, Path root,
                    String pattern) {
        return (T) Proxy.newProxyInstance(type.rawType().getClassLoader(),
            new Class<?>[] { type.rawType(), Proxyable.class },
            new ForwardingHandler(
                new DefaultFileTree<>(type, project, root, pattern)));
    }

    @Override
    public FileTree<T> withDirectories() {
        withDirs = true;
        return this;
    }

    @Override
    public FileTree<T> exclude(String pattern) {
        excludes.add(pattern);
        return this;
    }

    @Override
    public Path root(boolean relativize) {
        if (project == null) {
            return root.toAbsolutePath();
        }
        Path result = project.directory().resolve(root).normalize();
        if (relativize) {
            return project.directory().relativize(result);
        }
        return result;
    }

    @Override
    public Path root() {
        return root(false);
    }

    private void fill() {
        if (filled) {
            return;
        }
        try {
            find(root(), pattern);
        } catch (IOException e) {
            logger.atSevere().withCause(e).log("Problem scanning files");
            throw new BuildException(e);
        }
        filled = true;
    }

    @Override
    public Instant asOf() {
        fill();
        return latestChange;
    }

    @SuppressWarnings("PMD.CognitiveComplexity")
    private void find(Path root, String pattern) throws IOException {
        final PathMatcher pathMatcher = FileSystems.getDefault()
            .getPathMatcher("glob:" + pattern);
        final var excludeMatchers = excludes.parallelStream()
            .map(e -> FileSystems.getDefault()
                .getPathMatcher("glob:" + e))
            .toList();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path path,
                    BasicFileAttributes attrs) throws IOException {
                return testAndAdd(path);
            }

            private FileVisitResult testAndAdd(Path path) {
                if (excludeMatchers.parallelStream()
                    .filter(em -> em.matches(root.relativize(path)))
                    .findAny().isPresent()) {
                    if (path.toFile().isDirectory()) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                if (pathMatcher.matches(path)) {
                    @SuppressWarnings("unchecked")
                    T resource = (T) ResourceFactory
                        .create(type().containedType(), path);
                    DefaultFileTree.this.add(resource);
                    if (resource.asOf().isAfter(latestChange)) {
                        latestChange = resource.asOf();
                    }
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                if (withDirs) {
                    return testAndAdd(dir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                var dirMod = Instant.ofEpochMilli(dir.toFile().lastModified());
                if (dirMod.isAfter(latestChange)) {
                    latestChange = dirMod;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                if (exc instanceof AccessDeniedException) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public Stream<T> stream() {
        return StreamSupport
            .stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, 0) {

                private Iterator<T> theIterator;

                private Iterator<T> iterator() {
                    if (theIterator == null) {
                        fill();
                        theIterator = DefaultFileTree.super.stream().iterator();
                    }
                    return theIterator;
                }

                @Override
                public void forEachRemaining(Consumer<? super T> action) {
                    iterator().forEachRemaining(action);
                }

                @Override
                public boolean tryAdvance(Consumer<? super T> action) {
                    if (!iterator().hasNext()) {
                        return false;
                    }
                    action.accept(iterator().next());
                    return true;
                }
            }, false);
    }

    @Override
    public FileTree<T> clear() {
        super.clear();
        filled = false;
        return this;
    }

    @Override
    public FileTree<T> delete() {
        final PathMatcher pathMatcher = FileSystems.getDefault()
            .getPathMatcher("glob:" + pattern);
        try {
            var root = root();
            Files.walkFileTree(root, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path path,
                        BasicFileAttributes attrs) throws IOException {
                    if (pathMatcher.matches(path)) {
                        Files.delete(path);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir,
                        IOException exc) throws IOException {
                    if (exc != null) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (!dir.equals(root)
                        && Files.list(dir).findFirst().isEmpty()) {
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file,
                        IOException exc) throws IOException {
                    if (exc instanceof AccessDeniedException) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.atSevere().withCause(e).log("Problem scanning files");
            throw new BuildException(e);
        }
        filled = false;
        return this;
    }

    @Override
    public Stream<Path> entries() {
        return stream().map(fr -> root().relativize(fr.path()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result
            = prime * result + Objects.hash(excludes, pattern, root, withDirs);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        return (obj instanceof DefaultFileTree other)
            && Objects.equals(excludes, other.excludes)
            && Objects.equals(pattern, other.pattern)
            && Objects.equals(root, other.root) && withDirs == other.withDirs;
    }

    @Override
    public String toString() {
        var wasFilled = filled;
        fill();
        String str = type().toString() + " (" + asOfLocalized()
            + ") from " + Path.of("").toAbsolutePath().relativize(root())
            + " with " + stream().count() + " elements";
        if (!wasFilled) {
            clear();
        }
        filled = wasFilled;
        return str;
    }
}
