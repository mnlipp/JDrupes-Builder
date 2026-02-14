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

import java.io.PrintWriter;
import java.io.Writer;

/// An interface to a status line that can be used by [ResourceProvider]s
/// to indicate progress during the execution of
/// [ResourceProviderSpi#provide(ResourceRequest)].
///
public interface StatusLine extends AutoCloseable {

    /// An implementation that does nothing.
    StatusLine NOOP_STATUS_LINE = new StatusLine() {

        private final PrintWriter toVoid = new PrintWriter(Writer.nullWriter());

        @Override
        public void update(String text, Object... args) {
            // Does nothing
        }

        @Override
        public PrintWriter writer(String prefix) {
            return toVoid;
        }

        @Override
        public void close() {
            // Does nothing
        }
    };

    /// Update the text in the status line. If argumnts are given, `text`
    /// is interpreted as a format string for []String#format] with the
    /// given arguments.
    ///
    /// @param text the text
    /// @param args the arguments
    ///
    void update(String text, Object... args);

    /// Returns a writer to the status line. This writer can be used to
    /// append text to the status line. Closing the writer will not close
    /// the status line.
    /// 
    /// If a prefix is given, it will be printed at the beginning of the
    /// status line.
    ///
    /// @param prefix the prefix
    /// @return the prints the writer
    ///
    PrintWriter writer(String prefix);

    /// Deallocate the line for outputs from the current thread.
    ///
    @Override
    void close();

}