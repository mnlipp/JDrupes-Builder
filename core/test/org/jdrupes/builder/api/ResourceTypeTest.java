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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jdrupes.builder.api;

import static org.jdrupes.builder.api.ResourceType.*;
import static org.junit.Assert.assertTrue;
import org.junit.jupiter.api.Test;

public class ResourceTypeTest {

    @Test
    void testTypeAnalysis() {
        ResourceType<?> type = BaseFileTreeType;
        assertTrue(type.rawType().equals(FileTree.class));
        assertTrue(type.containedType().rawType().equals(FileResource.class));
    }

}
