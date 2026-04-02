/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package org.jdrupes.builder.api;

import static org.jdrupes.builder.api.ResourceType.*;
import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;

public class ResourceTypeTest {

    @Test
    void testBaseResourceType() {
        var base = BaseResourceType;
        assertEquals(Resource.class, base.rawType());
    }

    @Test
    void testFileResourceType() {
        var fileType = FileResourceType;
        assertEquals(FileResource.class, fileType.rawType());
    }

    @Test
    void testResourceFileType() {
        var resourceFileType = ResourceFileType;
        assertEquals(ResourceFile.class, resourceFileType.rawType());
    }

    @Test
    void testIOResourceType() {
        var ioResourceType = IOResourceType;
        assertEquals(IOResource.class, ioResourceType.rawType());
    }

    @Test
    void testIOResourcesType() {
        var ioResources = IOResourcesType;
        assertEquals(Resources.class, ioResources.rawType());
        assertEquals(IOResourceType, ioResources.containedType());
    }

    @Test
    void testBaseFileTreeHasContainedType() {
        assertTrue(BaseFileTreeType.containedType() != null);
    }

    @Test
    void testResourceFileTypeIsNotContainer() {
        assertNull(ResourceFileType.containedType());
    }

    @Test
    void testFileResourceTypeIsNotContainer() {
        assertNull(FileResourceType.containedType());
    }

    @Test
    void testIOResourceTypeIsNotContainer() {
        assertNull(IOResourceType.containedType());
    }

    @Test
    void testIOResourcesTypeHasContainedType() {
        assertNotNull(IOResourcesType.containedType());
    }

    @Test
    void testBaseResourceAssignableFromSubtype() {
        var base = BaseResourceType;
        assertTrue(base.isAssignableFrom(BaseFileTreeType));
    }

    @Test
    void testSubtypeIsAssignableFromSuper() {
        var sub = FileResourceType;
        var sup = BaseResourceType;
        assertTrue(sup.isAssignableFrom(sub));
    }

    @Test
    void testStaticMethodResourceType() {
        var resource = ResourceType.resourceType(FileResource.class);
        assertEquals(FileResource.class, resource.rawType());
        assertNull(resource.containedType());
    }

    @Test
    void testStaticMethodResourceTypeContainer() {  // throws Exception removed
        // Create a test resources container type
        var container
            = ResourceType.resourceType(FileTree.class, FileResourceType);
        assertEquals(FileTree.class, container.rawType());
        assertEquals(FileResource.class, container.containedType().rawType());
    }
}