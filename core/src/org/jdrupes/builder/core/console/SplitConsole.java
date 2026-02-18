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

import com.google.common.flogger.FluentLogger;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.jdrupes.builder.api.StatusLine;

/// Provides a split console using ANSI escape sequences.
///
@SuppressWarnings({ "PMD.AvoidSynchronizedStatement", "PMD.GodClass" })
public final class SplitConsole implements AutoCloseable {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private static final StatusLine NULL_STATUS_LINE = new StatusLine() {
        @Override
        public void update(String text, Object... args) {
            // Does nothing
        }

        @Override
        @SuppressWarnings("PMD.RelianceOnDefaultCharset")
        public PrintWriter writer(String prefix) {
            return new PrintWriter(OutputStream.nullOutputStream());
        }

        @Override
        public void close() {
            // Does nothing
        }
    };
    private static AtomicInteger openCount = new AtomicInteger();
    private static SplitConsole instance;
    private final TerminalInfo term;
    private final List<ManagedLine> managedLines = new ArrayList<>();
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    // Guarded by managedLines
    private final Map<Thread, String> offScreenLines = new LinkedHashMap<>();
    // Used to synchronize output to terminal
    private final PrintStream realOut;
    private final PrintStream splitOut;
    private final PrintStream splitErr;
    private byte[] incompleteLine = new byte[0];
    private final Redrawer redrawer;

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
    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition" })
    public static SplitConsole open() {
        synchronized (openCount) {
            if (openCount.incrementAndGet() == 1) {
                instance = new SplitConsole();
            }
            return instance;
        }
    }

    /// Return a status line implementation that discards all update
    /// information.
    ///
    /// @return the status line
    ///
    public static StatusLine nullStatusLine() {
        return NULL_STATUS_LINE;
    }

    /// Initializes a new split console.
    ///
    @SuppressWarnings({ "PMD.ForLoopCanBeForeach" })
    private SplitConsole() {
        logger.atFine().log("Initializing split console");
        realOut = System.out;

        this.term = new TerminalInfo();
        if (!term.supportsAnsi()) {
            logger.atFine().log("No ANSI terminal support, using plain output");
            redrawer = null;
            splitOut = realOut;
            splitErr = realOut;
            return;
        }

        logger.atFine().log("Using ANSI terminal support for split console");
        synchronized (managedLines) {
            recomputeLayout(term.lines() * 1 / 3);
            synchronized (realOut) {
                for (int i = 0; i < managedLines.size(); i++) {
                    realOut.println();
                }
                realOut.print(Ansi.cursorUp(managedLines.size()));
                realOut.flush();
            }
        }
        redrawer = new Redrawer();
        redrawStatus();
        splitOut = new PrintStream(new StreamWrapper(null), true,
            Charset.defaultCharset());
        splitErr = new PrintStream(new StreamWrapper(Ansi.Color.Red), true,
            Charset.defaultCharset());
        System.setOut(splitOut);
        System.setErr(splitErr);
    }

    private Optional<ManagedLine> managedLine(Thread thread) {
        Objects.nonNull(thread);
        return managedLines.stream()
            .filter(l -> Objects.equals(l.thread, thread))
            .findFirst();
    }

    /// Allocate a line for outputs from the current thread. 
    ///
    private void allocateLine() {
        if (openCount.get() == 0) {
            return;
        }
        Thread thread = Thread.currentThread();
        synchronized (managedLines) {
            if (managedLine(thread).isPresent()
                || offScreenLines.containsKey(thread)) {
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
    }

    private void initManaged(int index, Thread thread, String text) {
        var line = managedLines.get(index);
        line.thread = thread;
        line.text = text;
        line.lastRendered = null;
    }

    /// Deallocate the line for outputs from the current thread.
    ///
    private void deallocateLine() {
        Thread thread = Thread.currentThread();
        synchronized (managedLines) {
            managedLine(thread).ifPresentOrElse(line -> {
                line.thread = null;
                line.text = "";
                promoteOffScreenLines();
                redrawStatus();
            }, () -> offScreenLines.remove(thread));
        }
    }

    private void recomputeLayout(int managedHeight) {
        while (managedLines.size() > managedHeight) {
            if (managedLines.get(managedLines.size() - 1).thread == null) {
                managedLines.remove(managedLines.size() - 1);
                break;
            }
            int freeSlot;
            for (freeSlot = 0; freeSlot < managedLines.size() - 1;
                    freeSlot++) {
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
                // shiftManaged(i);
                var offLineIter = offScreenLines.entrySet().iterator();
                var offLine = offLineIter.next();
                initManaged(i, offLine.getKey(), offLine.getValue());
                offLineIter.remove();
            }
        }
    }

    /// Update the line for outputs from the current thread.
    ///
    /// @param text the text
    ///
    private void updateStatus(String text, Object... args) {
        Thread thread = Thread.currentThread();
        synchronized (managedLines) {
            managedLine(thread).ifPresentOrElse(line -> {
                if (args.length > 0) {
                    line.text = String.format(text, args);
                } else {
                    line.text = text;
                }
                logger.atFinest().log(
                    "Updated status for %s: %s", line.thread, line.text);
                redrawStatus();
            }, () -> {
                offScreenLines.put(thread, text);
            });
        }
    }

    /// Writes the bytes to the scrollable part of the console.
    ///
    /// @param text the text
    ///
    @SuppressWarnings({ "PMD.AvoidLiteralsInIfCondition" })
    private void write(byte[] text, int offset, int length, byte[] markup) {
        synchronized (realOut) {
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
                    writeMarkedup(realOut, text, offset, i - offset + 1,
                        markup);
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
        }
        redrawStatus(true);
    }

    private void write(byte[] text, byte[] markup) {
        write(text, 0, text.length, markup);
    }

    @SuppressWarnings("PMD.RelianceOnDefaultCharset")
    private void writeMarkedup(PrintStream out, byte[] chars, int off, int len,
            byte[] markup) {
        // Only called by write, already synchronized
        if (markup == null) {
            out.write(chars, off, len);
            return;
        }
        out.write(markup, 0, markup.length);
        out.write(chars, off, len);
        out.write(Ansi.resetAttributes().getBytes(), 0,
            Ansi.resetAttributes().length());
    }

    private void redrawStatus() {
        redrawStatus(false);
    }

    private void redrawStatus(boolean force) {
        if (!term.supportsAnsi()) {
            return;
        }
        if (redrawer != null) {
            redrawer.triggerRedraw(force);
        }
    }

    /// Redraws the status lines.
    /// 
    private final class Redrawer implements Runnable {
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final AtomicBoolean redraw = new AtomicBoolean(false);
        private final AtomicBoolean force = new AtomicBoolean(false);
        private final Thread thread;

        private Redrawer() {
            thread = Thread.ofVirtual().name("Split console redrawer")
                .start(this);
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    if (!running.get()) {
                        break;
                    }
                    if (!redraw.get()) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    redraw.set(false);
                }
                if (openCount.get() > 0) {
                    doRedraw(force.getAndSet(false));
                }
            }
        }

        private void triggerRedraw(boolean force) {
            synchronized (this) {
                redraw.set(true);
                if (force) {
                    this.force.set(true);
                }
                notifyAll();
            }
        }

        @SuppressWarnings({ "PMD.EmptyCatchBlock" })
        private void stop() {
            synchronized (this) {
                logger.atFine().log("Stopping redrawer");
                running.set(false);
                notifyAll();
            }
            try {
                thread.join();
                logger.atFine().log("Redrawer stopped");
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    @SuppressWarnings({ "PMD.ForLoopCanBeForeach" })
    private void doRedraw(boolean force) {
        synchronized (managedLines) {
            synchronized (realOut) {
                realOut.print(Ansi.hideCursor());
                for (int i = 0; i < managedLines.size(); i++) {
                    realOut.println();
                    ManagedLine line = managedLines.get(i);
                    if (!force
                        && Objects.equals(line.text, line.lastRendered)) {
                        continue;
                    }
                    realOut.print(Ansi.cursorToSol() + Ansi.clearLine());
                    if (openCount.get() > 0) {
                        realOut.print("> " + line.text.substring(0,
                            Math.min(line.text.length(), term.columns() - 2)));
                    }
                    line.lastRendered = line.text;
                }
                realOut.print(Ansi.cursorUp(managedLines.size())
                    + Ansi.cursorToSol() + Ansi.showCursor());
                if (incompleteLine.length > 0) {
                    realOut.write(incompleteLine, 0, incompleteLine.length);
                }
                realOut.flush();
            }
        }
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
        synchronized (openCount) {
            if (openCount.get() == 0) {
                return;
            }
            if (openCount.decrementAndGet() > 0) {
                return;
            }

            // Don't use this anymore
            instance = null;

            // Cleanup
            logger.atFine().log("Closing split console");
            if (redrawer != null) {
                redrawer.stop();
                synchronized (managedLines) {
                    for (var line : managedLines) {
                        line.thread = null;
                        line.text = "";
                        line.lastRendered = null;
                    }
                    offScreenLines.clear();
                }
                doRedraw(true);
            }
            System.setOut(realOut);
            logger.atFine().log("Split console closed");
        }
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

        /// Update.
        ///
        /// @param text the text
        /// @param args the args
        ///
        @Override
        public void update(String text, Object... args) {
            updateStatus(text, args);
        }

        /// Writer.
        ///
        /// @param prefix the prefix
        /// @return the prints the writer
        ///
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

        /// Write.
        ///
        /// @param ch the ch
        /// @throws IOException Signals that an I/O exception has occurred.
        ///
        @Override
        @SuppressWarnings("PMD.ShortVariable")
        public void write(int ch) throws IOException {
            SplitConsole.this.write(new byte[] { (byte) ch }, 0, 1, markup);
        }

        /// Write.
        ///
        /// @param ch the ch
        /// @param off the off
        /// @param len the len
        /// @throws IOException Signals that an I/O exception has occurred.
        ///
        @Override
        @SuppressWarnings("PMD.ShortVariable")
        public void write(byte[] ch, int off, int len) throws IOException {
            SplitConsole.this.write(ch, off, len, markup);
        }
    }

}
