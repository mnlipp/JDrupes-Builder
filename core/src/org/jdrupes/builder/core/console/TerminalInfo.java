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

package org.jdrupes.builder.core.console;

import java.io.InputStreamReader;

/// Provides information about the terminal. This class relies on variables
/// `LINES` and `COLUMNS` to be environment variables (exported).
///
public class TerminalInfo {
    private int lines;
    private int cols;
    private final boolean supportsAnsi;

    /// Initializes a new terminal info.
    ///
    public TerminalInfo() {
        this.supportsAnsi = System.console() != null
            && System.getenv("TERM") != null
            && !"dumb".equals(System.getenv("TERM"));
        refreshSize();
    }

    /// Returns whether the terminal supports ANSI.
    ///
    /// @return true, if successful
    ///
    public boolean supportsAnsi() {
        return supportsAnsi;
    }

    /// Returns the number of lines.
    ///
    /// @return the lines
    ///
    public int lines() {
        return lines;
    }

    /// Returns the number of columns.
    ///
    /// @return the columns
    ///
    public int columns() {
        return cols;
    }

    @SuppressWarnings({ "PMD.RelianceOnDefaultCharset",
        "PMD.AvoidCatchingGenericException", "PMD.ShortVariable",
        "PMD.AvoidLiteralsInIfCondition", "PMD.UnusedAssignment" })
    private void refreshSize() {
        try {
            Process proc
                = new ProcessBuilder("sh", "-c", "stty size < /dev/tty")
                    .redirectErrorStream(true).start();

            try (java.io.BufferedReader in = new java.io.BufferedReader(
                new InputStreamReader(proc.getInputStream()))) {
                String line = in.readLine();
                if (line != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 2) {
                        lines = Integer.parseInt(parts[0]);
                        cols = Integer.parseInt(parts[1]);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
            // use fallback
        }

        // fallback
        lines = 24;
        cols = 80;
    }
}