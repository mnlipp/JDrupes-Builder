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
import io.github.azagniotov.matcher.AntPathMatcher;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;

/// The default implementation of a [FileTree].
///
/// @param <T> the type of the [FileResource]s in the tree.
///
public class DefaultFileTree<T extends FileResource> extends DefaultResources<T>
        implements FileTree<T> {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final AntPathMatcher pathMatcher
        = new AntPathMatcher.Builder().build();
    private Instant latestChange;
    private final Project project;
    private final Path root;
    private final String[] patterns;
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
    ///
    /// @param type the resource type
    /// @param project the project
    /// @param root the root
    /// @param patterns the include patterns
    ///
    @SuppressWarnings({ "PMD.ArrayIsStoredDirectly", "PMD.UseVarargs" })
    protected DefaultFileTree(ResourceType<?> type, Project project, Path root,
            String[] patterns) {
        super(type);
        this.project = project;
        this.root = root;
        if (patterns.length == 0) {
            this.patterns = new String[] { "**/*" };
        } else {
            this.patterns = patterns;
        }
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
            find(root(), patterns);
        } catch (IOException e) {
            logger.atSevere().withCause(e).log("Problem scanning files");
            throw new BuildException().from(project).cause(e);
        }
        filled = true;
    }

    @Override
    public Optional<Instant> asOf() {
        fill();
        return Optional.ofNullable(latestChange);
    }

    @SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.UseVarargs" })
    private void find(Path root, String[] patterns) throws IOException {
        if (!root.toFile().exists()) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path path,
                    BasicFileAttributes attrs) throws IOException {
                return testAndAdd(path);
            }

            private FileVisitResult testAndAdd(Path path) {
                Path pathInTree = root.relativize(path);
                if (excludes.stream().anyMatch(ex -> pathMatcher
                    .isMatch(ex, pathInTree.toString()))) {
                    if (path.toFile().isDirectory()) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                if (Arrays.stream(patterns).anyMatch(pattern -> pathMatcher
                    .isMatch(pattern, pathInTree.toString()))) {
                    @SuppressWarnings("unchecked")
                    T resource = (T) ResourceFactory
                        .create(type().containedType(), path);
                    DefaultFileTree.this.add(resource);
                    if (resource.asOf().isPresent() && (latestChange == null
                        || resource.asOf().get().isAfter(latestChange))) {
                        latestChange = resource.asOf().get();
                    }
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
                if (!withDirs) {
                    return FileVisitResult.CONTINUE;
                }

                // Directories (and their modification date) included
                var dirMod = Instant.ofEpochMilli(dir.toFile().lastModified());
                if (latestChange == null || dirMod.isAfter(latestChange)) {
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
        return LazyCollectionStream.of(() -> {
            fill();
            return get();
        });
    }

    @Override
    public FileTree<T> clear() {
        super.clear();
        filled = false;
        return this;
    }

    @Override
    public void cleanup() {
        try {
            deleteFiles(root());
        } catch (IOException e) {
            logger.atSevere().withCause(e).log("Problem scanning files");
            throw new BuildException().from(project).cause(e);
        }
        filled = false;
    }

    @SuppressWarnings("PMD.CognitiveComplexity")
    private void deleteFiles(Path root) throws IOException {
        if (!root.toFile().exists()) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path path,
                    BasicFileAttributes attrs) throws IOException {
                return testAndDelete(path);
            }

            private FileVisitResult testAndDelete(Path path)
                    throws IOException {
                Path pathInTree = root.relativize(path);
                if (excludes.stream().anyMatch(ex -> pathMatcher
                    .isMatch(ex, pathInTree.toString()))) {
                    if (path.toFile().isDirectory()) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                if (Arrays.stream(patterns).anyMatch(pattern -> pathMatcher
                    .isMatch(pattern, pathInTree.toString()))) {
                    try {
                        Files.delete(path);
                    } catch (NoSuchFileException e) { // NOPMD
                        // We can have concurrent cleanups
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir,
                    IOException exc) throws IOException {
                if (exc != null) {
                    return FileVisitResult.CONTINUE;
                }
                if (dir.toFile().exists()) {
                    try {
                        if (Files.list(dir).findFirst().isEmpty()) {
                            Files.delete(dir);
                        }
                    } catch (NoSuchFileException e) { // NOPMD
                        // We can have concurrent cleanups
                    }
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
    }

    @Override
    public Stream<Path> paths() {
        return stream().map(fr -> root().relativize(fr.path()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<Entry<T>> entries() {
        return paths().map(path -> new Entry<T>(path,
            ResourceFactory.create((ResourceType<T>) type().containedType(),
                root().resolve(path))));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result
            = prime * result + Objects.hash(excludes, patterns, root, withDirs);
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
            && Objects.equals(patterns, other.patterns)
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
