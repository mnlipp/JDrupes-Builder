package org.jdrupes.builder.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jdrupes.builder.api.InputTree;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.ZipFile;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * JUnit tests for ZipFileInputTree.
 */
final class ZipFileInputTreeTest {

    @TempDir
    private Path tmpDir;
    private ZipFile zipFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create a test ZIP file programmatically for testing
        var zipPath = tmpDir.resolve("test.zip");
        try (var zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zos.putNextEntry(new ZipEntry("file.txt"));
            zos.write("Hello".getBytes());
            zos.closeEntry();
        }
        try (var zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zos.putNextEntry(new ZipEntry("sub/file.txt"));
            zos.write("Hello sub".getBytes());
            zos.closeEntry();
        }
        assertTrue(Files.exists(zipPath));
        zipFile = ZipFile.of(new ResourceType<ZipFile>() {}, zipPath);
    }

    @Test
    void testBasicRead() throws IOException {
        var tree = InputTree.of(zipFile, "**/*");
        assertNotNull(tree);
        var inStream = tree.entries()
            .filter(e -> e.path().equals(Path.of("sub/file.txt")))
            .findFirst().map(e -> e.resource().inputStream());
        assertTrue(inStream.isPresent());
        var content = new String(inStream.get().readAllBytes(), "utf-8");
        assertEquals("Hello sub", content);
    }
}
