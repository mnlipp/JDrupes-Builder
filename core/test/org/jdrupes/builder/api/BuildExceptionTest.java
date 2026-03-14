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

import org.jdrupes.builder.api.BuildException.Reason;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class BuildExceptionTest {

    @Test
    void testDefaultConstructor() {
        BuildException exception = new BuildException();
        assertEquals(Reason.Error, exception.reason());
        assertNull(exception.getMessage());
    }

    @Test
    void testReasonConstructor() {
        BuildException exception = new ConfigurationException();
        assertEquals(Reason.Configuration, exception.reason());
        assertNull(exception.getMessage());
    }

    @Test
    void testMessage() {
        BuildException exception = new BuildException();
        exception.message("Test message with %s and %d", "string", 42);
        assertEquals("Test message with string and 42", exception.getMessage());
    }

    @Test
    void testCause() {
        BuildException exception = new BuildException();
        Exception cause = new RuntimeException("Test cause");
        exception.cause(cause);
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testDetail() {
        BuildException exception = new BuildException();
        exception.detail("First detail\n");
        exception.detail("Second detail");
        assertEquals("First detail\nSecond detail", exception.details());
    }
}