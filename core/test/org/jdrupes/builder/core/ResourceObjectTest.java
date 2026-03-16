package org.jdrupes.builder.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * JUnit tests for ResourceObject.
 */
final class ResourceObjectTest {

    /**
     * Wrapper to allow instantiation.
     */
    private static final class ResourceObjectWrapper extends ResourceObject {
        public ResourceObjectWrapper() {
            super();
        }

        public ResourceObjectWrapper(String name) {
            name(name);
        }
    }

    @Test
    void testTypeReturnsClassBasedType() {
        ResourceObjectWrapper resource = new ResourceObjectWrapper();
        assertEquals(ResourceObjectWrapper.class, resource.type().rawType());
    }

    @Test
    void testNameIsEmptyByDefault() {
        ResourceObjectWrapper resource = new ResourceObjectWrapper();
        assertTrue(resource.name().isEmpty());
    }

    @Test
    void testEqualsLocksResource() {
        ResourceObjectWrapper resource = new ResourceObjectWrapper("test-name");
        assertTrue(resource.equals(resource));
        assertTrue(resource.isLocked());
    }

    @Test
    void testHashCodeLocksResource() {
        ResourceObjectWrapper resource = new ResourceObjectWrapper("test-name");
        resource.hashCode();
        assertTrue(resource.isLocked());
    }

    @Test
    void testToStringIncludesTypeAndName() throws Exception {
        ResourceObjectWrapper resource = new ResourceObjectWrapper();
        String result = resource.toString();
        assertTrue(result.contains("ResourceObject"));
    }

    @Test
    void testToStringIncludesNameWhenSet() throws Exception {
        ResourceObjectWrapper resource = new ResourceObjectWrapper();
        resource.name("test-resource");
        String result = resource.toString();
        assertTrue(result.contains("test-resource"));
    }

    @Test
    void testEqualsWithMatchingName() throws Exception {
        ResourceObjectWrapper resource1 = new ResourceObjectWrapper();
        ResourceObjectWrapper resource2 = new ResourceObjectWrapper();

        resource1.name("test-name");
        resource2.name("test-name");

        assertEquals(resource1, resource2);
    }

    @Test
    void testEqualsWithDifferentName() throws Exception {
        ResourceObjectWrapper resource1 = new ResourceObjectWrapper();
        ResourceObjectWrapper resource2 = new ResourceObjectWrapper();

        resource1.name("test-name");
        resource2.name("second-name");

        assertNotEquals(resource1, resource2);
    }
}
