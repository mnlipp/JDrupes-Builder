package org.jdrupes.builder.core;

import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AwaitableCounterTest {

    @Test
    void testInitialCount() {
        AwaitableCounter counter = new AwaitableCounter();
        assertNotNull(counter);
        // The count should start at 0
        assertEquals(0, counter.value());
    }

    @Test
    void testIncrementDecrement() {
        AwaitableCounter counter = new AwaitableCounter();

        assertEquals(0, counter.value());

        counter.increment();
        assertEquals(1, counter.value());

        counter.increment();
        assertEquals(2, counter.value());

        counter.decrement();
        assertEquals(1, counter.value());

        counter.decrement();
        assertEquals(0, counter.value());
    }

    @Test
    void testIncrementAutoClose() {
        AwaitableCounter counter = new AwaitableCounter();

        // Test that Increment implements AutoCloseable correctly
        try (AwaitableCounter.Increment _ = counter.acquire()) {
            assertEquals(1, counter.value());
        }
        // The Increment should have been closed automatically, so counter should
        // be 0
        assertEquals(0, counter.value());
    }

    @Test
    void testAwaitZero() throws InterruptedException {
        AwaitableCounter counter = new AwaitableCounter();

        // Test waiting for a specific count value
        counter.increment();
        counter.increment(); // Count is now 2

        Thread waiter = new Thread(() -> {
            try {
                counter.await(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        waiter.start();
        Thread.sleep(10);
        assertTrue(waiter.isAlive()); // Should still be alive as count is 2

        counter.decrement();
        Thread.sleep(10);
        assertTrue(waiter.isAlive()); // Should still be alive as count is 1

        counter.decrement(); // Now count is 0
        assertTrue(waiter.join(Duration.ofMillis(10)));
    }

    @Test
    void testMultipleIncrements() {
        AwaitableCounter counter = new AwaitableCounter();

        try (AwaitableCounter.Increment _ = counter.acquire()) {
            assertEquals(1, counter.value());

            try (AwaitableCounter.Increment _ = counter.acquire()) {
                assertEquals(2, counter.value());
            }
            // i2 is closed, so count should be 1
            assertEquals(1, counter.value());
        }
        // i1 is closed, so count should be 0
        assertEquals(0, counter.value());
    }

    @Test
    void testIncrementConstruction() {
        AwaitableCounter counter = new AwaitableCounter();

        // Test that creating an Increment via the acquire method increases the
        // count
        AwaitableCounter.Increment increment = counter.acquire();
        assertEquals(1, counter.value());

        increment.close(); // This should bring the count back to 0
        assertEquals(0, counter.value());
    }
}