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
import org.jdrupes.builder.api.Project;

// TODO: Auto-generated Javadoc
/// The representation of a file set.
/// The Class FileSet.
///
///
public class FileSet extends ResourceSet<FileResource> {

    private String kind = KIND_UNKNOWN;
    private Instant newestFile = Instant.MIN;
    private final Project project;
    private final Path root;
    private final String pattern;
    private boolean filled;

    /// Instantiates a new file set. The file set includes all files
    /// matching `pattern` in the tree starting at `root`. `root`
    /// may be specified as absolute path or as path relative to the
    /// `project`'s directory (see [Project#directory]).
    ///
    /// @param project the project
    /// @param root the root of the file tree to search for files matching
    /// `pattern`
    /// @param pattern the pattern
    /// Instantiates a new file set.
    ///
    /// @param project the project
    /// @param root the root
    /// @param pattern the pattern
    ///
    ///
    public FileSet(Project project, Path root, String pattern) {
        this.project = project;
        this.root = project.directory().resolve(root);
        this.pattern = pattern;
    }

    /// Returns the kind of this file set (as resource).
    ///
    /// @param kind the kind
    /// @return the file set
    /// Kind.
    ///
    /// @param kind the kind
    /// @return the file set
    ///
    ///
    public FileSet kind(String kind) {
        this.kind = kind;
        return this;
    }

    /// Kind.
    ///
    /// @return the string
    ///
    @Override
    public String kind() {
        return kind;
    }

    /// Returns the root of the file tree searched for files.
    ///
    /// @param relativize whether to return a path relative to the project's
    /// directory
    /// @return the path
    /// Root.
    ///
    /// @param relativize the relativize
    /// @return the path
    ///
    ///
    public Path root(boolean relativize) {
        if (relativize) {
            return project.directory().relativize(root);
        }
        return root;
    }

    /// Returns the root of the file tree searched for files
    /// as an absolute path.
    ///
    /// @return the path
    /// Root.
    ///
    /// @return the path
    ///
    ///
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

    /// As of.
    ///
    /// @return the instant
    ///
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
                    var resource = new FileResource(path);
                    FileSet.this.add(resource);
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

    /// Stream.
    ///
    /// @return the stream
    ///
    @Override
    public Stream<FileResource> stream() {
        fill();
        return super.stream();
    }

    /// Deletes all files in this file set and directories that are
    /// empty after deletion of the files (expect for root, which is
    /// not deleted).
    ///
    public void delete() {
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
    }

    /// To string.
    ///
    /// @return the string
    ///
    @Override
    public String toString() {
        fill();
        return "FileSet (kind " + kind() + ") from "
            + project.rootProject().relativize(root())
            + " with " + content.size() + " files, newest: " + newestFile;
    }
}
