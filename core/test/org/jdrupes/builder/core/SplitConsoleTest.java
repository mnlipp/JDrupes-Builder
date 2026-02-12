package org.jdrupes.builder.core;

import org.jdrupes.builder.core.console.SplitConsole;

public class SplitConsoleTest {

    public static void main(String[] args) throws InterruptedException {
        SplitConsole console = SplitConsole.open();

        int threadCount = 25;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                try (var status = console.statusLine()) {
                    Thread.sleep(idx * 10 + 1);
                    for (int j = 1; j <= 5 + idx; j++) {
                        status.update("Task " + idx + " step " + j);
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Worker-" + i);
            threads[i].start();
        }

        for (int i = 0; i < 30; i++) {
            console.out().println("Main thread step " + i);
            Thread.sleep(300);
        }

        // Wait for all threads to finish
        for (Thread t : threads) {
            t.join();
        }

        // Print a message in the bottom 1/3 scrolling area
        console.out().println("All tasks completed!");
        console.close();
    }
}
