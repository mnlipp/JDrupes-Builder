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

/// Provides ANSI escape sequences.
///
@SuppressWarnings({ "PMD.ShortClassName" })
public final class Ansi {
    private static final String PREFIX = "\u001B[";

    private Ansi() {
        // Make javadoc happy
    }

    /// Cursor to start of line.
    ///
    /// @return the string
    ///
    public static String cursorToSol() {
        return PREFIX + "G";
    }

    /// Cursor up.
    ///
    /// @param lines the lines
    /// @return the string
    ///
    public static String cursorUp(int lines) {
        return PREFIX + lines + "A";
    }

    /// Cursor down.
    ///
    /// @param lines the lines
    /// @return the string
    ///
    public static String cursorDown(int lines) {
        return PREFIX + lines + "B";
    }

//    /// Sets the scroll range.
//    ///
//    /// @param first the first line
//    /// @param last the last line
//    /// @return the string
//    ///
//    public static String scrollRange(int first, int last) {
//        return PREFIX + (first + 1) + ";" + (last + 1) + "r";
//    }
//
//    /// Clears the scroll range.
//    ///
//    /// @return the string
//    ///
//    public static String scrollAll() {
//        return PREFIX + "r";
//    }
//
//    /// Scroll one line up.
//    ///
//    /// @return the string
//    ///
//    public static String scrollUp() {
//        return PREFIX + "1S";
//    }

    /// Clear the current line.
    ///
    /// @return the string
    ///
    public static String clearLine() {
        return PREFIX + "2K";
    }

    /// Hide cursor.
    ///
    /// @return the string
    ///
    public static String hideCursor() {
        return PREFIX + "?25l";
    }

    /// Show the cursor.
    ///
    /// @return the string
    ///
    public static String showCursor() {
        return PREFIX + "?25h";
    }

    /// Sets a color.
    ///
    /// @param color the color
    /// @return the string
    ///
    public static String color(Color color) {
        return PREFIX + color.code() + "m";
    }

    /// Reset attributes.
    ///
    /// @return the string
    ///
    public static String resetAttributes() {
        return PREFIX + "0m";
    }
    
    /// Color arguments for [#color].
    ///
    @SuppressWarnings("PMD.FieldNamingConventions")
    public enum Color {
        /// The color red.
        Red(31),
        /// The color green.
        Green(32),
        /// The color yellow.
        Yellow(33),
        /// The color blue.
        Blue(34),
        /// The color magenta.
        Magenta(35),
        /// The color cyan.
        Cyan(36);

        private final int code;

        Color(int code) {
            this.code = code;
        }

        /// Code.
        ///
        /// @return the int
        ///
        public int code() {
            return code;
        }
    }

}