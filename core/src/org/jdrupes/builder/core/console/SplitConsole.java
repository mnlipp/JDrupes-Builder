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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import org.jdrupes.builder.api.StatusLine;

/// Provides a split console using ANSI escape sequences.
///
@SuppressWarnings("PMD.GodClass")
public final class SplitConsole implements AutoCloseable {

    private static int openCount;
    private static SplitConsole instance;
    private final TerminalInfo term;
    private final List<ManagedLine> managedLines = new ArrayList<>();
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<Thread, String> offScreenLines = new LinkedHashMap<>();
    private final PrintStream realOut;
    private final PrintStream splitOut;
    private final PrintStream splitErr;
    private final AtomicBoolean startRedraw = new AtomicBoolean();
    private byte[] incompleteLine = new byte[0];
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private final Thread redrawThread = Thread.ofVirtual()
        .start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (startRedraw) {
                    boolean run = startRedraw.compareAndSet(true, false);
                    if (!run) {
                        try {
                            startRedraw.wait();
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                doRedraw();
            }
        });

    /// The Class ManagedLine.
    ///
    @SuppressWarnings({ "PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal" })
    private class ManagedLine {
        private Thread thread;
        private String text = "";
        private String lastRendered;
    }

    /// Open the split console.
    ///
    /// @return the split console
    ///
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    public static SplitConsole open() {
        synchronized (SplitConsole.class) {
            if (instance == null) {
                instance = new SplitConsole();
            }
        }
        openCount += 1;
        return instance;
    }

    /// Initializes a new split console.
    ///
    @SuppressWarnings("PMD.ForLoopCanBeForeach")
    private SplitConsole() {
        realOut = System.out;

        this.term = new TerminalInfo();
        if (!term.supportsAnsi()) {
            splitOut = realOut;
            splitErr = realOut;
            return;
        }

        recomputeLayout(term.lines() * 1 / 3);
        for (int i = 0; i < managedLines.size(); i++) {
            realOut.println();
        }
        realOut.print(Ansi.cursorUp(managedLines.size()));
        realOut.flush();
        redraw();
        splitOut = new PrintStream(new StreamWrapper(null), true,
            Charset.defaultCharset());
        splitErr = new PrintStream(new StreamWrapper(Ansi.Color.Red), true,
            Charset.defaultCharset());
        System.setOut(splitOut);
        System.setErr(splitErr);
    }

    private ManagedLine managedLine(Thread thread) {
        return managedLines.stream()
            .filter(l -> Objects.equals(l.thread, thread))
            .findFirst().orElse(null);
    }

    /// Allocate a line for outputs from the current thread. 
    ///
    @SuppressWarnings({ "PMD.AvoidSynchronizedAtMethodLevel",
        "PMD.AvoidDuplicateLiterals" })
    private synchronized void allocateLine() {
        Thread thread = Thread.currentThread();
        if (managedLine(thread) != null || offScreenLines.containsKey(thread)) {
            return;
        }

        // Find free and allocate or use overflow
        IntStream.range(0, managedLines.size())
            .filter(i -> managedLines.get(i).thread == null).findFirst()
            .ifPresentOrElse(i -> {
                initManaged(i, thread, "");
            }, () -> {
                offScreenLines.put(thread, "");
            });
    }

    private void initManaged(int index, Thread thread, String text) {
        var line = managedLines.get(index);
        line.thread = thread;
        line.text = text;
    }

    /// Deallocate the line for outputs from the current thread.
    ///
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    private synchronized void deallocateLine() {
        Thread thread = Thread.currentThread();
        var line = managedLine(thread);
        if (line != null) {
            line.thread = null;
            line.text = "";
            promoteOffScreenLines();
            redraw();
        } else {
            offScreenLines.remove(thread);
        }
    }

    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    private synchronized void recomputeLayout(int managedHeight) {
        while (managedLines.size() > managedHeight) {
            if (managedLines.get(managedLines.size() - 1).thread == null) {
                managedLines.remove(managedLines.size() - 1);
                break;
            }
            int freeSlot;
            for (freeSlot = 0; freeSlot < managedLines.size() - 1; freeSlot++) {
                if (managedLines.get(freeSlot).thread == null) {
                    break;
                }
            }
            if (freeSlot < managedLines.size() - 1) {
                ManagedLine last = managedLines.get(managedLines.size() - 1);
                managedLines.set(freeSlot, last);
                continue;
            }
            ManagedLine last = managedLines.get(managedLines.size() - 1);
            offScreenLines.put(last.thread, last.text);
            last.thread = null;
        }

        if (managedLines.size() < managedHeight) {
            while (managedLines.size() < managedHeight) {
                managedLines.add(new ManagedLine());
            }
            promoteOffScreenLines();
        }
    }

    private void promoteOffScreenLines() {
        for (int i = 0; i < managedLines.size(); i++) {
            if (offScreenLines.isEmpty()) {
                break;
            }
            if (managedLines.get(i).thread == null) {
                shiftManaged(i);
                var offLine = offScreenLines.entrySet().iterator().next();
                initManaged(managedLines.size() - 1, offLine.getKey(),
                    offLine.getValue());
                offScreenLines.remove(offLine.getKey());
            }
        }
    }

    private void shiftManaged(int index) {
        var lastManaged = managedLines.size() - 1;
        for (int i = index; i < lastManaged; i++) {
            var toMove = managedLines.get(i + 1);
            managedLines.set(i, toMove);
            toMove.lastRendered = null;
        }
        managedLines.set(lastManaged, new ManagedLine());
    }

    /// Update the line for outputs from the current thread.
    ///
    /// @param text the text
    ///
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    private synchronized void updateStatus(String text, Object... args) {
        Thread thread = Thread.currentThread();
        var line = managedLine(thread);

        if (line != null) {
            if (args.length > 0) {
                line.text = String.format(text, args);
            } else {
                line.text = text;
            }
            redraw();
        } else if (offScreenLines.containsKey(thread)) {
            offScreenLines.put(thread, text);
        }
    }

    /// Writes the bytes to the scrollable part of the console.
    ///
    /// @param text the text
    ///
    @SuppressWarnings({ "PMD.AvoidSynchronizedAtMethodLevel",
        "PMD.AvoidLiteralsInIfCondition" })
    private synchronized void write(byte[] text, int offset, int length,
            byte[] markup) {
        if (incompleteLine.length > 0) {
            // Prepend left over text and try again
            byte[] prepended = new byte[incompleteLine.length + length];
            System.arraycopy(
                incompleteLine, 0, prepended, 0, incompleteLine.length);
            System.arraycopy(
                text, offset, prepended, incompleteLine.length, length);
            incompleteLine = new byte[0];
            write(prepended, markup);
            return;
        }

        // Write line(s)
        realOut.print(Ansi.cursorToSol() + Ansi.clearLine());
        int end = offset + length;
        for (int i = offset; i < end; i++) {
            if (text[i] == '\n') {
                // Write line including newline, moves cursor to next line
                writeMarkedup(realOut, text, offset, i - offset + 1, markup);
                offset = i + 1;
                // Clear left over text from status line
                realOut.print(Ansi.clearLine());
            }
            if (i - offset >= term.columns()) {
                // Write line up to here
                writeMarkedup(realOut, text, offset, i - offset, markup);
                offset = i;
                // Write newline an clear left over text from status line
                realOut.print("\n" + Ansi.clearLine());
            }
        }
        incompleteLine = new byte[end - offset];
        System.arraycopy(text, offset, incompleteLine, 0, end - offset);
        if (incompleteLine.length > 0) {
            // Show incomplete line, will be overwritten by next write
            realOut.write(incompleteLine, 0, incompleteLine.length);
        }
        realOut.flush();

        // Redraw all
        for (var line : managedLines) {
            line.lastRendered = null;
        }
        redraw();
    }

    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    private synchronized void write(byte[] text, byte[] markup) {
        write(text, 0, text.length, markup);
    }

    @SuppressWarnings("PMD.RelianceOnDefaultCharset")
    private void writeMarkedup(PrintStream out, byte[] chars, int off, int len,
            byte[] markup) {
        if (markup == null) {
            out.write(chars, off, len);
            return;
        }
        out.write(markup, 0, markup.length);
        out.write(chars, off, len);
        out.write(Ansi.resetAttributes().getBytes(), 0,
            Ansi.resetAttributes().length());
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private void redraw() {
        if (!term.supportsAnsi()) {
            return;
        }
        synchronized (startRedraw) {
            startRedraw.set(true);
            startRedraw.notifyAll();
        }
    }

    @SuppressWarnings({ "PMD.AvoidSynchronizedAtMethodLevel",
        "PMD.ForLoopCanBeForeach" })
    private synchronized void doRedraw() {
        realOut.print(Ansi.hideCursor());
        for (int i = 0; i < managedLines.size(); i++) {
            realOut.println();
            ManagedLine line = managedLines.get(i);
            if (!Objects.equals(line.text, line.lastRendered)) {
                realOut.print(Ansi.cursorToSol() + Ansi.clearLine()
                    + line.text.substring(0,
                        Math.min(line.text.length(), term.columns())));
                line.lastRendered = line.text;
            }
        }
        realOut.print(Ansi.cursorUp(managedLines.size()) + Ansi.cursorToSol()
            + Ansi.showCursor());
        if (incompleteLine.length > 0) {
            realOut.write(incompleteLine, 0, incompleteLine.length);
        }
        realOut.flush();
    }

    /// Writes the bytes to the scrollable part of the console.
    ///
    /// @return the prints the stream
    ///
    public PrintStream out() {
        return splitOut;
    }

    /// Writes the bytes to the scrollable part of the console.
    ///
    /// @return the prints the stream
    ///
    public PrintStream err() {
        return splitErr;
    }

    /// Close.
    ///
    @Override
    public void close() {
        redrawThread.interrupt();
        try {
            redrawThread.join();
        } catch (InterruptedException e) { // NOPMD
            // Ignore, really
        }
        openCount -= 1;
        if (openCount > 0 || !term.supportsAnsi()) {
            return;
        }
        realOut.print(Ansi.showCursor());
        System.setOut(realOut);
        instance = null;
    }

    /// Allocates a line for outputs from the current thread.
    ///
    /// @return the status line
    ///
    public DefaultStatusLine statusLine() {
        return new DefaultStatusLine();
    }

    /// Represents a status line for outputs from the current thread.
    ///
    public final class DefaultStatusLine implements StatusLine {

        private PrintWriter asWriter;

        /// Initializes a new status line for the invoking thread.
        ///
        private DefaultStatusLine() {
            allocateLine();
        }

        @Override
        public void update(String text, Object... args) {
            updateStatus(text, args);
        }

        @Override
        public PrintWriter writer(String prefix) {
            if (asWriter == null) {
                asWriter = new PrintWriter(new Writer() {
                    private final String prepend = prefix == null ? "" : prefix;
                    @SuppressWarnings("PMD.AvoidStringBufferField")
                    private final StringBuilder buf = new StringBuilder();

                    @Override
                    public void write(char[] cbuf, int off, int len)
                            throws IOException {
                        for (int i = off; i < off + len; i++) {
                            if (cbuf[i] == '\n' || cbuf[i] == '\r') {
                                updateStatus(prepend + buf.toString());
                                buf.setLength(0);
                            } else {
                                buf.append(cbuf[i]);
                            }
                        }
                    }

                    @Override
                    public void flush() throws IOException {
                        updateStatus(prepend + buf.toString());
                    }

                    @Override
                    public void close() throws IOException {
                        // Does nothing
                    }

                });
            }
            return asWriter;
        }

        /// Deallocate the line for outputs from the current thread.
        ///
        @Override
        public void close() {
            deallocateLine();
        }
    }

    /// Makes [#write] available as a stream.
    ///
    public final class StreamWrapper extends OutputStream {

        private final byte[] markup;

        private StreamWrapper(Ansi.Color color) {
            markup = Optional.ofNullable(color).map(Ansi::color)
                .map(String::getBytes).orElse(null);
        }

        @Override
        @SuppressWarnings("PMD.ShortVariable")
        public void write(int ch) throws IOException {
            SplitConsole.this.write(new byte[] { (byte) ch }, 0, 1, markup);
        }

        @Override
        @SuppressWarnings("PMD.ShortVariable")
        public void write(byte[] ch, int off, int len) throws IOException {
            SplitConsole.this.write(ch, off, len, markup);
        }
    }

}
