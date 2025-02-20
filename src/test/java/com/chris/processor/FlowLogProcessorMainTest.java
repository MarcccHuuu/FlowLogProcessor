package com.chris.processor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;

/**
 * Test Class for FlowLogProcessorMain
 *
 * @author Chris
 * @date 2/20/25
 */
@ExtendWith(MockitoExtension.class)
class FlowLogProcessorMainTest {

    @TempDir
    Path tempDir;

    private Path lookupFile;
    private Path outputFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary lookup file
        lookupFile = tempDir.resolve("lookup_table.csv");
        Files.write(lookupFile, List.of(
                "Port,Protocol,Tag",  // Header
                "80,TCP,Web Traffic",
                "53,UDP,DNS Query",
                "22,TCP,SSH"
        ));

        // Output file for testing writeOutPutFile()
        outputFile = tempDir.resolve("output.csv");
    }

    @Test
    @DisplayName("Test main method")
    void testMainMethod2() throws Exception {
        // Define file paths and mock content
        String flowLogFile = "attachments/input/flow_logs.txt";
        String lookupFile = "attachments/input/lookup_table.csv";
        String tagOutputFile = "attachments/output/tag_counts.csv";
        String portProtocolOutputFile = "attachments/output/port_protocol_counts.csv";
        List<String> mockFlowLogLines = Collections.singletonList("192.168.1.1,80,TCP");
        List<String> mockLookupLines = List.of("Port,Protocol,Tag", "80,TCP,Web");


        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // Mock to get mocked content
            mockedFiles.when(() -> Files.readAllLines(Paths.get(flowLogFile)))
                    .thenReturn(mockFlowLogLines);
            mockedFiles.when(() -> Files.readAllLines(Paths.get(lookupFile)))
                    .thenReturn(mockLookupLines);

            // Mock writing
            BufferedWriter mockWriter = mock(BufferedWriter.class);
            mockedFiles.when(() -> Files.newBufferedWriter(Paths.get(tagOutputFile))).thenReturn(mockWriter);
            mockedFiles.when(() -> Files.newBufferedWriter(Paths.get(portProtocolOutputFile))).thenReturn(mockWriter);

            // Act and Assert
            Assertions.assertDoesNotThrow(() -> FlowLogProcessorMain.main(new String[]{}));
            verify(mockWriter, atLeastOnce()).write(anyString());
        }
    }

    @Test
    void testReadLookupTable() throws Exception {
        Map<String, String> lookupTable = invokeReadLookupTable(lookupFile.toString());

        // Check if lookup table read
        assertEquals(3, lookupTable.size());
        assertEquals("Web Traffic", lookupTable.get("80,tcp"));
        assertEquals("DNS Query", lookupTable.get("53,udp"));
        assertEquals("SSH", lookupTable.get("22,tcp"));
    }

    @Test
    void testReadLookupTableWithInvalid() throws Exception {
        // Create a
        Path invalidLookupFile = tempDir.resolve("invalid_lookup.csv");
        Files.write(invalidLookupFile, List.of(
                "Port,Protocol,Tag",
                "80,TCP,tag1",
                "Invalid",
                "53,UDP,tag2"
        ));

        Map<String, String> lookupTable = invokeReadLookupTable(invalidLookupFile.toString());

        // The invalid will be ignored
        assertEquals(2, lookupTable.size());
        assertEquals("tag1", lookupTable.get("80,tcp"));
        assertEquals("tag2", lookupTable.get("53,udp"));
    }

    private Map<String, String> invokeReadLookupTable(String path) throws Exception {
        Method method = FlowLogProcessorMain.class.getDeclaredMethod("readLookupTable", String.class);
        method.setAccessible(true);
        return (Map<String, String>) method.invoke(null, path);
    }

    @Test
    void testWriteOutPutFile() throws Exception {
        Map<String, Integer> sampleData = new HashMap<>();
        sampleData.put("80,tcp", 10);
        sampleData.put("53,udp", 5);

        invokeWriteOutPutFile(outputFile.toString(), sampleData, "Port,Protocol,Count");

        // Assert
        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(3, lines.size());
        assertEquals("Port,Protocol,Count", lines.get(0));
        assertTrue(lines.contains("80,tcp,10"));
        assertTrue(lines.contains("53,udp,5"));
    }

    @Test
    void testWriteOutPutFileEmptyData() throws Exception {
        Map<String, Integer> emptyData = new HashMap<>();

        invokeWriteOutPutFile(outputFile.toString(), emptyData, "Header");

        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(1, lines.size());
        assertEquals("Header", lines.get(0));
    }

    private void invokeWriteOutPutFile(String path, Map<String, Integer> data, String header) throws Exception {
        Method method = FlowLogProcessorMain.class.getDeclaredMethod("writeOutPutFile", String.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(null, path, data, header);
    }
}