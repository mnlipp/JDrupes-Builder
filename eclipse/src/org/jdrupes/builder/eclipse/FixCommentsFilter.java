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

package org.jdrupes.builder.eclipse;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/// Add a comment to the written properties file and remove any
/// comments from upstream. This seems to be the only possible
/// way to avoid a time stamp in the written properties file
/// which may lead to confusion if nothing else has changed.
///
public class FixCommentsFilter extends FilterWriter {

    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder buffer = new StringBuilder();

    /// Instantiates a new fix comments filter.
    ///
    /// @param out the out
    /// @param comment the comment
    ///
    public FixCommentsFilter(Writer out, String comment) {
        super(out);
        if (comment != null) {
            Arrays.stream(comment.split("\n")).map(String::trim)
                .forEach(s -> {
                    try {
                        out.append("# ").append(s)
                            .append(System.lineSeparator());
                    } catch (IOException e) { // NOPMD
                        // Not really important
                    }
                });
        }
    }

    @Override
    public void write(int chr) throws IOException {
        buffer.append((char) chr);
        checkBuffer();
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        buffer.append(cbuf, off, len);
        checkBuffer();
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        buffer.append(str, off, len);
        checkBuffer();
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private void checkBuffer() throws IOException {
        while (true) {
            int idx = buffer.indexOf("\n");
            if (idx < 0) {
                break;
            }
            if (buffer.charAt(0) != '#') {
                out.write(buffer.substring(0, idx + 1));
            }
            buffer.delete(0, idx + 1);
        }
    }

    @Override
    public void close() throws IOException {
        out.write(buffer.toString());
        out.close();
    }
}
