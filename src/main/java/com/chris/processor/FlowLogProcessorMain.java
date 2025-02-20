package com.chris.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Main Entrypoint Class to execute the flow log processor
 *
 * @author Chris
 * @date 2/20/25
 */
public class FlowLogProcessorMain {

    private static final Logger logger = Logger.getLogger(FlowLogProcessorMain.class.getName());

    private static final String INPUT_FILE_FOLDER = "attachments/input/";

    private static final String OUTPUT_FILE_FOLDER = "attachments/output/";

    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * Main Method to start Flow Log Processor
     *
     * read input files, process logs using multi-threading, and write output reports.
     */
    public static void main(String[] args) {
        String flowLogFile = INPUT_FILE_FOLDER + "flow_logs.txt";
        String lookupFile = INPUT_FILE_FOLDER + "lookup_table.csv";
        String tagCountOutputFile = OUTPUT_FILE_FOLDER + "tag_counts.csv";
        String portProtocolCountOutputFile = OUTPUT_FILE_FOLDER + "port_protocol_counts.csv";

        try {
            // Read input files into buffer
            ConcurrentHashMap<String, String> lookupTable = new ConcurrentHashMap<>(readLookupTable(lookupFile));
            List<String> flowLogLines = Files.readAllLines(Paths.get(flowLogFile));

            // Define the output data in thread-safe map
            ConcurrentHashMap<String, Integer> tagCounts = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Integer> portProtocolCounts = new ConcurrentHashMap<>();

            // Create a fixed-size thread pool
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            List<Future<?>> futures = new ArrayList<>();

            // Divide log lines into batches for parallel processing
            int batchSize = flowLogLines.size() / THREAD_COUNT;
            for (int i = 0; i < THREAD_COUNT; i++) {
                int start = i * batchSize;
                int end = (i == THREAD_COUNT - 1) ? flowLogLines.size() : (start + batchSize);
                List<String> logBatch = flowLogLines.subList(start, end);

                FlowLogHandler handler = new FlowLogHandler(logBatch, lookupTable, tagCounts, portProtocolCounts);
                futures.add(executor.submit(handler));
            }

            // Wait for all tasks to complete and then shutdown
            for (Future<?> future : futures) {
                future.get();
            }
            executor.shutdown();

            // Output the report data
            writeOutPutFile(tagCountOutputFile, tagCounts, "Tag,Count");
            writeOutPutFile(portProtocolCountOutputFile, portProtocolCounts, "Port,Protocol,Count");

            logger.info("Flow Log File Processing Finished successfully. Reports Generated.");

        } catch (Exception e) {
            logger.severe(String.format("Flow Log File Processing Error: %s", e.getMessage()));
        }
    }

    /**
     * Read Lookup table and put matching principles into a map
     *
     * @param lookupFilePath the file path of lookup table
     * @return {@code Map<String, String>}, key is "Port,Protocol", value is the corresponding Tag.
     * @throws IOException io error occurs when reading the file
     */
    private static Map<String, String> readLookupTable(String lookupFilePath) throws IOException {
        Map<String, String> lookUpMap = new HashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(lookupFilePath));

        // Skip the header and Process each line
        for (String line : lines.subList(1, lines.size())) {
            String[] data = line.split(",");
            if (data.length < 3) {
                logger.warning(String.format("The entry in lookUp table is not in the right format: %s", line));
                continue;
            }

            // Build unique key with port and protocol (ignore case)
            String key = data[0].trim() + "," + data[1].trim().toLowerCase();
            String tag = data[2].trim();
            lookUpMap.put(key, tag);
        }
        return lookUpMap;
    }

    /**
     * Write output into file
     *
     * @param fileName the file path of output report
     * @param data the content of the file
     * @param columnHeader the column header of output csv file
     * @throws IOException io error occurs when reading the file
     */
    private static void writeOutPutFile(String fileName, Map<String, Integer> data,
                                        String columnHeader) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
            writer.write(columnHeader);
            writer.newLine();
            // Write each entry in the map to the file
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
        }
    }

}