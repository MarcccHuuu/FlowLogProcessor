package com.chris.processor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler Class for each executor in multi-thread.
 *
 * It is responsible for processing a subset of flow logs
 * and inserting the thread-safe map for tag counts and port-protocol counts.
 *
 * @author Chris
 * @date 2/20/25
 */
public class FlowLogHandler implements Runnable {

    private final List<String> logBatch;
    private final Map<String, String> lookupTable;
    private final ConcurrentHashMap<String, Integer> tagCounts;
    private final ConcurrentHashMap<String, Integer> portProtocolCounts;

    /**
     * Constructor to initialize FlowLogHandler
     *
     * @param logBatch List of log entries to process (subset)
     * @param lookupTable Map of "Port,Protocol" to "Tag" in Lookup table
     * @param tagCounts Thread-safe map for tag counts
     * @param portProtocolCounts Thread-safe map for port-protocol counts
     */
    public FlowLogHandler(List<String> logBatch, Map<String, String> lookupTable,
                          ConcurrentHashMap<String, Integer> tagCounts,
                          ConcurrentHashMap<String, Integer> portProtocolCounts) {
        this.logBatch = logBatch;
        this.lookupTable = lookupTable;
        this.tagCounts = tagCounts;
        this.portProtocolCounts = portProtocolCounts;
    }

    /**
     * Executing Method for Multi-Thread.
     * process flow logs and inserting the thread-safe map for tag counts and port-protocol counts.
     */
    @Override
    public void run() {
        for (String line : logBatch) {
            String[] data = line.split(" ");
            if (data.length < 8) {
                continue;
            }

            // Decode destination port, protocol number and Protocol from log entry
            String dstPort = data[5];
            String protocolNum = data[7];
            String protocol = translateProtocol(protocolNum);
            if (protocol == null) continue;

            // Build the combination in the format "Port,Protocol", and increase count in map
            // Map the tag from lookup table (default "Untagged"), and increase count in map
            String combination = dstPort + "," + protocol;
            String tag = lookupTable.getOrDefault(combination, "Untagged");
            portProtocolCounts.merge(combination, 1, Integer::sum);
            tagCounts.merge(tag, 1, Integer::sum);
        }
    }

    /**
     * Translate the digit in log to corresponding protocol
     *
     * @param number digit in the log entry
     * @return result protocol
     */
    private static String translateProtocol(String number) {
        return switch (number) {
            case "6" -> "tcp";
            case "17" -> "udp";
            case "1" -> "icmp";
            default -> null;
        };
    }

}
