package org.jdrupes.builder.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultFileTreeTest {

    @TempDir
    private Path tmpDir;

    @Test
    void testBasicFileDiscoveryAndEntries() throws IOException {
        // create two files
        Path f1 = tmpDir.resolve("r1.txt");
        Path f2 = tmpDir.resolve("r2.txt");
        Files.writeString(f1, "one");
        Files.writeString(f2, "two");

        // use pattern to match all files under tmpDir
        FileTree<FileResource> ft = FileTree.of(null, tmpDir, "**/*.txt");

        // stream should discover files
        List<String> names = ft.stream()
            .map(fr -> fr.name().orElse(""))
            .collect(Collectors.toList());
        // resource names may include relative path segments, check suffixes
        assertTrue(names.stream().anyMatch(n -> n.endsWith("r1.txt")));
        assertTrue(names.stream().anyMatch(n -> n.endsWith("r2.txt")));

        // entries should be relative paths
        List<Path> entries = ft.entries().collect(Collectors.toList());
        assertEquals(2, entries.size());
        assertTrue(entries.stream()
            .anyMatch(p -> p.getFileName().toString().equals("r1.txt")));
    }

    @Test
    void testMultiIncludes() throws IOException {
        Path sub = tmpDir.resolve("subdir");
        Files.createDirectories(sub);
        Path f1 = sub.resolve("a.txt");
        Path other = tmpDir.resolve("otherdir");
        Files.createDirectories(other);
        Path f2 = other.resolve("b.txt");
        Path f3 = other.resolve("c.txt");
        Files.writeString(f1, "a");
        Files.writeString(f2, "b");
        Files.writeString(f3, "c");

        FileTree<FileResource> ft = FileTree.of(null, tmpDir,
            "subdir/a.txt", "otherdir/b.txt");
        List<String> names = ft.stream().map(fr -> fr.name().orElse(""))
            .collect(Collectors.toList());
        assertEquals(2, names.stream().count());
        assertTrue(names.stream().anyMatch(n -> n.endsWith("a.txt")));
        assertTrue(names.stream().anyMatch(n -> n.endsWith("b.txt")));
    }

    @Test
    void testExcludesAndDirectories() throws IOException {
        Path sub = tmpDir.resolve("subdir");
        Files.createDirectories(sub);
        Path f1 = sub.resolve("a.txt");
        // create a second subdirectory that should not be excluded
        Path other = tmpDir.resolve("otherdir");
        Files.createDirectories(other);
        Path f3 = other.resolve("c.txt");
        // create a file in the root as well
        Path f2 = tmpDir.resolve("b.txt");
        Files.writeString(f1, "a");
        Files.writeString(f2, "b");
        Files.writeString(f3, "c");

        FileTree<FileResource> ft = FileTree.of(null, tmpDir, "**/*.txt");
        // exclude the subdir's files
        ft.exclude("subdir/**");
        List<String> names = ft.stream().map(fr -> fr.name().orElse(""))
            .collect(Collectors.toList());
        // resources may return relative paths (e.g., ../../../b.txt),
        // so check suffix: root file and otherdir file should be present,
        // subdir file should be excluded
        assertTrue(names.stream().anyMatch(n -> n.endsWith("b.txt")));
        assertTrue(names.stream().anyMatch(n -> n.endsWith("c.txt")));
        assertFalse(names.stream().anyMatch(n -> n.endsWith("a.txt")));

        // now verify directory reporting when directories are explicitly
        // matched create a file tree that matches directories as well
        // (pattern "**/*")
        FileTree<FileResource> ftDirs = FileTree.of(null, tmpDir, "**/*");
        ftDirs.exclude("subdir/**");
        ftDirs.withDirectories();
        List<String> namesWithDirs
            = ftDirs.stream().map(fr -> fr.name().orElse(""))
                .collect(Collectors.toList());
        // should contain otherdir as subdir was excluded
        assertTrue(namesWithDirs.stream()
            .anyMatch(n -> n.endsWith("otherdir") || n.endsWith("otherdir/")));
    }

    @Test
    void testDeleteRemovesFilesAndEmptyDirs() throws IOException {
        Path sub = tmpDir.resolve("sub");
        Files.createDirectories(sub);
        Path f1 = sub.resolve("a.txt");
        Path f2 = tmpDir.resolve("b.txt");
        Files.writeString(f1, "a");
        Files.writeString(f2, "b");

        FileTree<FileResource> ft = FileTree.of(null, tmpDir, "**/*.txt");

        // trigger fill
        assertTrue(ft.stream().findAny().isPresent());

        ft.cleanup();

        // files should be gone
        assertFalse(Files.exists(f1));
        assertFalse(Files.exists(f2));
        // empty subdir should have been removed
        assertFalse(Files.exists(sub));
    }

    @Test
    void testAsOfReflectsLatestChange()
            throws IOException, InterruptedException {
        Path f1 = tmpDir.resolve("x.txt");
        Files.writeString(f1, "x");

        FileTree<FileResource> ft = FileTree.of(null, tmpDir, "**/*.txt");

        @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
        Instant first = ft.asOf().get();
        // touch file to update modified time
        Thread.sleep(3);
        Files.writeString(f1, "xx");
        // clear so next asOf recalculates
        ft.clear();
        Instant second = ft.asOf().get();
        assertTrue(second.isAfter(first));
    }

    @Test
    void testAsOfForEmptyDirectory() throws IOException {
        FileTree<FileResource> ft = FileTree.of(null, tmpDir, "**/*.txt");
        
        // For an empty directory, asOf() should return Optional.empty()
        assertFalse(ft.asOf().isPresent());
    }
}