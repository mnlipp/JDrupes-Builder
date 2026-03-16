package org.jdrupes.builder.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jdrupes.builder.api.ConfigurationException;
import static org.jdrupes.builder.api.ResourceType.FileResourceType;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DefaultFileResourceTest {

    /**
     * Inner class to allow instantiation of DefaultFileResource for testing.
     */
    private static final class DefaultFileResourceWrapper
            extends DefaultFileResource {
        public DefaultFileResourceWrapper() {
            super(FileResourceType, Path.of(""));
        }

        public DefaultFileResourceWrapper(Path path) {
            super(FileResourceType, path);
        }
    }

    private Path tmpDir = null;

    @BeforeEach
    void setUp() throws Exception {
        tmpDir = Files.createTempDirectory("default-fileresource-test-");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tmpDir != null && Files.exists(tmpDir)) {
            Files.walk(tmpDir).sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception e) {
                    // ignore
                }
            });
        }
    }

    @Test
    void testConstructionWithValidAbsolutePath() throws Exception {
        String filename = UUID.randomUUID().toString();
        Path file = tmpDir.resolve(filename);

        DefaultFileResourceWrapper resource
            = new DefaultFileResourceWrapper(file);
        assertNotNull(resource);
    }

    @Test
    void testPathReturnsCorrectPath() throws Exception {
        String filename = UUID.randomUUID().toString();
        Path file = tmpDir.resolve(filename);

        DefaultFileResourceWrapper resource
            = new DefaultFileResourceWrapper(file);
        assertEquals(file, resource.path());
    }

    @Test
    void testAsOfReturnsEmptyWhenFileDoesNotExist() throws Exception {
        String filename = UUID.randomUUID().toString();
        Path file = tmpDir.resolve(filename);

        DefaultFileResourceWrapper resource
            = new DefaultFileResourceWrapper(file);
        assertFalse(resource.asOf().isPresent());
    }

    @Test
    void testAsOfReturnsModifiedTimeWhenFileExists() throws Exception {
        String filename = UUID.randomUUID().toString();
        Path file = tmpDir.resolve(filename);
        Files.writeString(file, "test content");

        DefaultFileResourceWrapper resource
            = new DefaultFileResourceWrapper(file);
        assertTrue(resource.asOf().isPresent());
    }

    @Test
    void testAsOfReturnsSameModifiedTime() throws Exception {
        String filename = UUID.randomUUID().toString();
        Path file = tmpDir.resolve(filename);
        Files.writeString(file, "test content");

        DefaultFileResourceWrapper resource1
            = new DefaultFileResourceWrapper(file);
        Optional<Instant> first = resource1.asOf();

        DefaultFileResourceWrapper resource2
            = new DefaultFileResourceWrapper(file);
        Optional<Instant> second = resource2.asOf();

        assertTrue(first.equals(second));
    }

    @Test
    void testAsOfReflectsFileModificationAfterWrite() throws Exception {
        String filename = UUID.randomUUID().toString();
        Path file = tmpDir.resolve(filename);
        Files.writeString(file, "test content");

        DefaultFileResourceWrapper resource
            = new DefaultFileResourceWrapper(file);
        Optional<Instant> first = resource.asOf();

        Thread.sleep(10);
        Files.writeString(file, "modified content");

        DefaultFileResourceWrapper updatedResource
            = new DefaultFileResourceWrapper(file);
        Optional<Instant> second = updatedResource.asOf();

        assertTrue(second.get().isAfter(first.get()));
    }

    @Test
    void testInputStreamRetrievesContent() throws Exception {
        String filename = UUID.randomUUID().toString();
        Path file = tmpDir.resolve(filename);
        Files.writeString(file, "test content");

        DefaultFileResourceWrapper resource
            = new DefaultFileResourceWrapper(file);
        try (var inputStream = resource.inputStream()) {
            byte[] buffer = new byte[14];
            int bytesRead = inputStream.read(buffer);
            assertNotNull(buffer);
            assertEquals("test content", new String(buffer, 0, bytesRead));
        }
    }

    @Test
    void testOutputStreamCreatesEmptyFile() throws Exception {
        String filename = UUID.randomUUID().toString();
        Path file = tmpDir.resolve(filename);

        DefaultFileResourceWrapper resource
            = new DefaultFileResourceWrapper(file);
        try (var outputStream = resource.outputStream()) {
            assertNotNull(outputStream);
        }
        assertTrue(Files.exists(file));
    }

    @Test
    void testOutputStreamWritesBytes() throws Exception {
        String filename = UUID.randomUUID().toString();
        Path file = tmpDir.resolve(filename);

        DefaultFileResourceWrapper resource
            = new DefaultFileResourceWrapper(file);
        try (var outputStream = resource.outputStream()) {
            outputStream.write("test".getBytes());
        }

        assertEquals("test", Files.readString(file));
    }

    @Test
    void testConstructionWithRelativePathThrowsException() throws Exception {
        String filename = UUID.randomUUID().toString();
        Path file = Path.of(filename); // This is a relative path

        assertThrows(ConfigurationException.class, () -> {
            new DefaultFileResourceWrapper(file);
        }, "Expected doThing() to throw, but it didn't");
    }
}
