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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import org.jdrupes.builder.api.StatusLine;

/// Provides a split console using ANSI escape sequences.
///
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
    private final AtomicBoolean needsRedraw = new AtomicBoolean();
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private final Thread redrawThread = Thread.ofVirtual()
        .start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (needsRedraw) {
                    boolean run = needsRedraw.compareAndSet(true, false);
                    if (!run) {
                        try {
                            needsRedraw.wait();
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
    /// @param format the text
    ///
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    private synchronized void updateStatus(String format, Object... args) {
        Thread thread = Thread.currentThread();
        var line = managedLine(thread);

        if (line != null) {
            line.text = String.format(format, args);
            redraw();
        } else if (offScreenLines.containsKey(thread)) {
            offScreenLines.put(thread, format);
        }
    }

    /// Writes the bytes to the scrollable part of the console.
    ///
    /// @param text the text
    ///
    @SuppressWarnings({ "PMD.AvoidSynchronizedAtMethodLevel",
        "PMD.AvoidLiteralsInIfCondition" })
    private synchronized void write(byte[] text, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            realOut.write(text[i]);
            if (text[i] == '\n') {
                // Clear status line
                realOut.print(Ansi.clearLine());
            }
        }
        realOut.flush();

        // Redraw all
        for (var line : managedLines) {
            line.lastRendered = null;
        }
        redraw();
    }

    private void write(byte[] text) {
        write(text, 0, text.length);
    }

    @SuppressWarnings({ "PMD.AvoidSynchronizedAtMethodLevel",
        "PMD.AvoidSynchronizedStatement" })
    private synchronized void redraw() {
        if (!term.supportsAnsi()) {
            return;
        }
        synchronized (needsRedraw) {
            needsRedraw.set(true);
            needsRedraw.notifyAll();
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

        /// Initializes a new status line for the invoking thread.
        ///
        private DefaultStatusLine() {
            allocateLine();
        }

        /// Update the text in the status line.
        ///
        /// @param format the text
        /// @param args the arguments
        ///
        @Override
        public void update(String format, Object... args) {
            updateStatus(format, args);
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

        private final Ansi.Color color;

        private StreamWrapper(Ansi.Color color) {
            this.color = color;
        }

        @SuppressWarnings("PMD.RelianceOnDefaultCharset")
        private byte[] markup(byte[] chars, int off, int len)
                throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.writeBytes(Ansi.color(color).getBytes());
            baos.write(chars, off, len);
            baos.writeBytes(Ansi.resetAttributes().getBytes());
            return baos.toByteArray();
        }

        @Override
        @SuppressWarnings("PMD.ShortVariable")
        public void write(int ch) throws IOException {
            if (color != null) {
                SplitConsole.this.write(markup(new byte[] { (byte) ch }, 0, 1));
                return;
            }
            SplitConsole.this.write(new byte[] { (byte) ch }, 0, 1);
        }

        @Override
        @SuppressWarnings("PMD.ShortVariable")
        public void write(byte[] ch, int off, int len) throws IOException {
            if (color != null) {
                SplitConsole.this.write(markup(ch, off, len));
                return;
            }
            SplitConsole.this.write(ch, off, len);
        }
    }

}
