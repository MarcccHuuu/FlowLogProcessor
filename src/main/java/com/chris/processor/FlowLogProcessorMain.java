package com.chris.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main Entrypoint Class to execute the flow log processor
 *
 * @author Chris
 * @date 2/20/25
 */
public class FlowLogProcessorMain {

    private static final String INPUT_FILE_FOLDER = "attachments/input/";

    private static final String OUTPUT_FILE_FOLDER = "attachments/output/";

    public static void main(String[] args) {
        String flowLogFile = INPUT_FILE_FOLDER + "flow_logs.txt";
        String lookupFile = INPUT_FILE_FOLDER + "lookup_table.csv";
        String tagCountOutputFile = OUTPUT_FILE_FOLDER + "tag_counts.csv";
        String portProtocolCountOutputFile = OUTPUT_FILE_FOLDER + "port_protocol_counts.csv";

        try {
            ConcurrentHashMap<String, String> lookupTable = new ConcurrentHashMap<>(readLookupTable(lookupFile));
            List<String> flowLogLines = Files.readAllLines(Paths.get(flowLogFile));

            ConcurrentHashMap<String, Integer> tagCounts = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, Integer> portProtocolCounts = new ConcurrentHashMap<>();

            writeOutPutFile(tagCountOutputFile, tagCounts, "Tag,Count");
            writeOutPutFile(portProtocolCountOutputFile, portProtocolCounts, "Port,Protocol,Count");
        } catch (IOException e) {
            e.printStackTrace();
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
        for (String line : lines.subList(1, lines.size())) {
            String[] data = line.split(",");
            if (data.length < 3) {
                continue;
            }
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
     */
    private static void writeOutPutFile(String fileName, Map<String, Integer> data, String columnHeader) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
            writer.write(columnHeader);
            writer.newLine();
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
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