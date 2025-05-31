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

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;

/// The default implementation of a [FileTree].
///
/// @param <T> the type of the [FileResource]s in the tree.
///
public class DefaultFileTree<T extends FileResource> extends DefaultResources<T>
        implements FileTree<T> {

    private Instant latestChange = Instant.MIN;
    private final Project project;
    private final Path root;
    private final String pattern;
    private final boolean withDirs;
    private final Class<T> leafType;
    private boolean filled;

    /// Returns a new file tree. The file tree includes all files
    /// matching `pattern` in the tree starting at `root`. `root`
    /// may be specified as absolute path or as path relative to the
    /// `project`'s directory (see [Project#directory]).
    ///
    /// @param project the project
    /// @param root the root of the file tree to search for files matching
    /// `pattern`
    /// @param pattern the pattern
    /// @param leafType the type of the elements in the tree
    /// @param withDirs whether to include directories
    ///
    public DefaultFileTree(Project project, Path root,
            String pattern, Class<T> leafType, boolean withDirs) {
        super(FileTree.class);
        this.project = project;
        if (project == null) {
            this.root = root.toAbsolutePath();
        } else {
            this.root = project.directory().resolve(root).normalize();
        }
        this.pattern = pattern;
        this.withDirs = withDirs;
        this.leafType = leafType;
    }

    @Override
    public Path root(boolean relativize) {
        if (relativize && project != null) {
            return project.directory().relativize(root);
        }
        return root;
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
            find(root, pattern);
        } catch (IOException e) {
            log.log(java.util.logging.Level.SEVERE, e,
                () -> "Problem scanning files: " + e.getMessage());
            throw new BuildException(e);
        }
        filled = true;
    }

    @Override
    public Instant asOf() {
        fill();
        return latestChange;
    }

    private void find(Path root, String pattern)
            throws IOException {
        final PathMatcher pathMatcher = FileSystems.getDefault()
            .getPathMatcher("glob:" + pattern);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path path,
                    BasicFileAttributes attrs) throws IOException {
                testAndAdd(path);
                return FileVisitResult.CONTINUE;
            }

            private void testAndAdd(Path path) {
                if (pathMatcher.matches(path)) {
                    T resource = DefaultFileResource.create(leafType, path);
                    DefaultFileTree.this.add(resource);
                    if (resource.asOf().isAfter(latestChange)) {
                        latestChange = resource.asOf();
                    }
                }
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                if (withDirs) {
                    testAndAdd(dir);
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
        fill();
        return super.stream();
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
            log.log(java.util.logging.Level.SEVERE, e,
                () -> "Problem scanning files: " + e.getMessage());
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
        result = prime * result + Objects.hash(pattern, root);
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        DefaultFileTree<?> other = (DefaultFileTree<?>) obj;
        return Objects.equals(pattern, other.pattern)
            && Objects.equals(root, other.root);
    }

    @Override
    public String toString() {
        fill();
        return "FileSet (type " + type().getSimpleName() + ") from "
            + (project != null
                ? project.rootProject().directory().relativize(root)
                : root)
            + " with " + stream().count() + " files, newest: " + latestChange;
    }
}
