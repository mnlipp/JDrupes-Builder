package org.jdrupes.builder.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * JUnit tests for StreamCollector.
 */
final class StreamCollectorTest {

    @Test
    void testUncachedCollectorCanBeCreated() {
        var collector = StreamCollector.uncached();
        assertNotNull(collector);
    }

    @Test
    void testCachedCollectorCanBeCreated() {
        var collector = StreamCollector.cached();
        assertNotNull(collector);
    }

    @Test
    void testAddWithSingleStreamAddsOneSource() {
        List<Integer> items = Arrays.asList(1, 2, 3);
        var collector = StreamCollector.uncached();
        collector.add(items.stream());
        assertEquals(3L, collector.stream().count());
    }

    @Test
    void testAddWithMultipleStreamsCombinesAll() {
        List<Integer> items1 = Arrays.asList(1, 2, 3);
        List<Integer> items2 = Arrays.asList(4, 5, 6);
        var collector = StreamCollector.uncached();
        collector.add(items1.stream());
        collector.add(items2.stream());
        assertEquals(6L, collector.stream().count());
    }

    @Test
    void testAddWithVarargsItemsCreatesStreamFromVarargs() {
        var collector = StreamCollector.uncached();
        collector.add(10, 20, 30);
        assertEquals(3L, collector.stream().count());
    }

    @Test
    void testAddAfterCallingStreamThrowsException() {
        var collector = StreamCollector.uncached();
        List<Integer> items = Arrays.asList(1, 2, 3);
        collector.add(items.stream());
        collector.stream(); // Consumes collection

        assertThrows(IllegalStateException.class, () -> {
            collector.add(4, 5, 6);
        }, "Cannot add sources after stream() has been called.");
    }

    @Test
    void testCachedCollectorReturnsSameStreamOnMultipleInvocations() {
        List<Integer> data = List.of(1, 2, 3, 4, 5);
        var collector = StreamCollector.cached(data.stream());

        assertEquals(5L, collector.stream().count());
        assertEquals(5L, collector.stream().count()); // Still cached
        assertEquals(5L, collector.stream().count()); // Still cached
    }

    @Test
    void testUncachedCollectorConsumesSources() {
        List<Integer> data = List.of(1, 2, 3, 4, 5);
        var collector = StreamCollector.uncached(data.stream());

        assertEquals(5L, collector.stream().count());
        // Second call should throw since sources consumed
        assertThrows(NullPointerException.class, () -> {
            collector.stream().count();
        }, "Cannot call stream() more than once.");
    }

    @Test
    void testCachedCollectorWithNulls() {
        var collector = StreamCollector.uncached();
        List<Object> objects = Arrays.asList((Object) null);
        collector.add(objects.stream());
        assertEquals(1L, collector.stream().count());
    }

    @Test
    void testUncachedMultipleInvocationsAfterFirst() {
        List<Integer> items = Arrays.asList(7, 8, 9);
        var collector = StreamCollector.uncached(items.stream());

        assertEquals(3L, collector.stream().count());
        assertThrows(NullPointerException.class, () -> {
            collector.stream(); // Second call after consumption
        });
    }

    @Test
    void testAddWithEmptyStream() {
        var collector = StreamCollector.uncached();
        List<Integer> empty = Collections.emptyList();
        collector.add(empty.stream());
        assertEquals(0L, collector.stream().count());
    }

    @Test
    void testFlattensMixedStreamAndVarargs() {
        var collector = StreamCollector.uncached();
        collector.add(1, 2, 3); // Varargs
        collector.add(Arrays.asList(4, 5, 6).stream()); // Stream
        List<Object> result = collector.stream().collect(Collectors.toList());
        assertEquals(6L, result.size());
    }

    @Test
    void testCollectionOrderIsPreserved() {
        List<Integer> items1 = Arrays.asList(100, 200, 300);
        List<Integer> items2 = Arrays.asList(400, 500, 600);

        StreamCollector<Integer> collector = StreamCollector.uncached();
        collector.add(items1.stream());
        collector.add(items2.stream());

        List<Integer> result = collector.stream().collect(Collectors.toList());
        assertEquals(100L, result.get(0).longValue());
        assertEquals(600L, result.get(result.size() - 1).longValue());
    }

    @Test
    void testCachedCollectorCanAddMultipleStreamsToCache() {
        List<Integer> first = Arrays.asList(1, 2);
        List<Integer> second = Arrays.asList(3, 4);

        var collector = StreamCollector.cached();
        collector.add(first.stream());
        collector.add(second.stream());

        assertEquals(4L, collector.stream().count());
    }

    @Test
    void testCachedCollectorWithEmptyInitial() {
        var collector = StreamCollector.cached();
        // No streams added initially - empty cache is ok
        assertEquals(0L, collector.stream().count());
    }
}
