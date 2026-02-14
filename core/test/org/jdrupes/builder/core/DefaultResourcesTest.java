package org.jdrupes.builder.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jdrupes.builder.api.IOResource;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DefaultResourcesTest {

    static class DummyIOResource extends ResourceObject implements IOResource {
        private final String name;
        private final Instant asOf;
        private final byte[] data;

        DummyIOResource(String name) {
            this.name = name;
            this.asOf = Instant.now();
            this.data = name.getBytes();
        }

        @Override
        public InputStream inputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public OutputStream outputStream() throws IOException {
            return new ByteArrayOutputStream();
        }

        @Override
        public Optional<Instant> asOf() {
            return Optional.of(asOf);
        }

        @Override
        public org.jdrupes.builder.api.ResourceType<?> type() {
            return IOResourceType;
        }

        @Override
        public java.util.Optional<String> name() {
            return java.util.Optional.ofNullable(name);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Objects.hash(name);
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
            if (!(obj instanceof DummyIOResource)) {
                return false;
            }
            DummyIOResource other = (DummyIOResource) obj;
            return Objects.equals(name, other.name);
        }

    }

    @Test
    void testAddStreamAndClear() {
        CoreResourceFactory factory = new CoreResourceFactory();

        // Create a Resources<IOResource> via the factory
        Resources<IOResource> resources = factory.newResource(
            IOResourcesType, null).orElseThrow();
        assertTrue(resources.isEmpty());

        // Create two dummy IOResource instances via helper class
        IOResource r1 = new DummyIOResource("r1");
        IOResource r2 = new DummyIOResource("r2");

        resources.add(r1).add(r2);
        assertFalse(resources.isEmpty());

        List<String> names
            = resources.stream().map(res -> res.name().orElse(""))
                .collect(Collectors.toList());
        assertEquals(List.of("r1", "r2"), names,
            "Stream should preserve insertion order");

        Resources<IOResource> copy = factory
            .newResource(IOResourcesType, null).orElseThrow();
        // Add same elements in same order via helper-created resources
        IOResource r1b = new DummyIOResource("r1");
        IOResource r2b = new DummyIOResource("r2");
        copy.add(r1b).add(r2b);

        assertEquals(resources, copy,
            "Two resource sets with same contents should be equal");
        assertEquals(resources.hashCode(), copy.hashCode(),
            "hashCode must match for equal sets");

        String asString = resources.toString();
        assertTrue(asString.contains("with 2 elements"));

        resources.clear();
        assertTrue(resources.isEmpty(), "clear should remove all elements");
    }

}