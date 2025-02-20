package com.chris.processor;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Class for FlowLogHandler
 *
 * @author Chris
 * @date 2/20/25
 */
@ExtendWith(MockitoExtension.class)
class FlowLogHandlerTest {

    private List<String> batch;
    private Map<String, String> lookupTable;
    private ConcurrentHashMap<String, Integer> tagCounts;
    private ConcurrentHashMap<String, Integer> portProtocolCounts;

    @BeforeEach
    void setUp() {
        batch = List.of(
                "2 2024-02-20T12:00:00Z src-ip dst-ip 1234 80 5678 6",  // TCP
                "2 2024-02-20T12:01:00Z src-ip dst-ip 1234 53 5678 17", // UDP
                "invalid log line without enough fields"             // Invalid
        );

        lookupTable = Map.of(
                "80,tcp", "tag1",
                "53,udp", "tag2"
        );

        tagCounts = new ConcurrentHashMap<>();
        portProtocolCounts = new ConcurrentHashMap<>();
    }

    @Test
    void testRunProcessesValidLogs() {
        FlowLogHandler handler = new FlowLogHandler(batch, lookupTable, tagCounts, portProtocolCounts);
        handler.run();

        // Verify the portProtocolCounts updates
        assertEquals(1, portProtocolCounts.getOrDefault("80,tcp", 0));
        assertEquals(1, portProtocolCounts.getOrDefault("53,udp", 0));

        // Verify the tagCounts updates
        assertEquals(1, tagCounts.getOrDefault("tag1", 0));
        assertEquals(1, tagCounts.getOrDefault("tag2", 0));
    }

    @Test
    void testRunHandlesInvalidLogLinesGracefully() {
        FlowLogHandler handler = new FlowLogHandler(batch, lookupTable, tagCounts, portProtocolCounts);
        handler.run();

        // Verify invalid log line does not update maps
        assertFalse(portProtocolCounts.containsKey("invalid"));
        assertFalse(tagCounts.containsKey("invalid"));
    }

    @Test
    void testDefaultTagAssignment() {
        batch = List.of("2 123456789012 eni-2d2e2f3g 192.168.2.7 77.88.55.80 49153 993 6 7 3500 1620140661 1620140721 ACCEPT OK");
        FlowLogHandler handler = new FlowLogHandler(batch, lookupTable, tagCounts, portProtocolCounts);
        handler.run();

        // Check that untagged data is recorded
        assertEquals(1, tagCounts.getOrDefault("Untagged", 0));
    }

    @Test
    void testMultipleThreadsSafety() throws InterruptedException {
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(new FlowLogHandler(batch, lookupTable, tagCounts, portProtocolCounts));
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Each log should be processed by 10 threads
        assertEquals(numThreads, portProtocolCounts.getOrDefault("80,tcp", 0));
        assertEquals(numThreads, portProtocolCounts.getOrDefault("53,udp", 0));
        assertEquals(numThreads, tagCounts.getOrDefault("tag1", 0));
        assertEquals(numThreads, tagCounts.getOrDefault("tag2", 0));
    }
}