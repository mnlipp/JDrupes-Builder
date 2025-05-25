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
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Project;

/// The default implementation of a [FileTree].
///
public class DefaultFileTree extends ResourceSet<FileResource>
        implements FileTree {

    private String kind = KIND_UNKNOWN;
    private Instant newestFile = Instant.MIN;
    private final Project project;
    private final Path root;
    private final String pattern;
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
    ///
    public DefaultFileTree(Project project, Path root, String pattern) {
        this.project = project;
        this.root = project.directory().resolve(root);
        this.pattern = pattern;
    }

    /// Returns a new file tree. The file tree includes all files
    /// matching `pattern` in the tree starting at `root`. `root`
    /// may must specified as absolute path.
    ///
    /// @param root the root of the file tree to search for files matching
    /// `pattern`
    /// @param pattern the pattern
    ///
    public DefaultFileTree(Path root, String pattern) {
        project = null;
        this.root = root.toAbsolutePath();
        this.pattern = pattern;
    }

    /// Sets the kind of this file set (as resource).
    ///
    /// @param kind the kind
    /// @return the file set
    ///
    public FileTree kind(String kind) {
        this.kind = kind;
        return this;
    }

    @Override
    public String kind() {
        return kind;
    }

    /// Returns the root of the file tree searched for files.
    ///
    /// @param relativize whether to return a path relative to the project's
    /// directory
    /// @return the path
    ///
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
        return newestFile;
    }

    private void find(Path root, String pattern)
            throws IOException {
        final PathMatcher pathMatcher = FileSystems.getDefault()
            .getPathMatcher("glob:" + pattern);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path path,
                    BasicFileAttributes attrs) throws IOException {
                if (pathMatcher.matches(path)) {
                    var resource = new DefaultFileResource(path);
                    DefaultFileTree.this.add(resource);
                    if (resource.asOf().isAfter(newestFile)) {
                        newestFile = resource.asOf();
                    }
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
    public Stream<FileResource> stream() {
        fill();
        return super.stream();
    }

    @Override
    public FileTree delete() {
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
                        IOException exc)
                        throws IOException {
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

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        fill();
        return "FileSet (kind " + kind() + ") from " + root(true)
            + " with " + content.size() + " files, newest: " + newestFile;
    }
}
