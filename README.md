# Flow Log Processor (Multi-threaded)

## Overview
This project is a **multi-threaded** Java application that processes flow logs(TXT file), applies tag mappings from
a lookup table(CSV file), and generates summary statistics.

The program is optimized for performance using multi-threading, ensuring efficient processing for large log files (up to 10MB).

## Installation & Usage

### 1. Prerequisites

Java - JDK 17

Maven - for dependency management, introducing junit test module.

Install them using Homebrew with command line:
```
brew install maven
```

### 2.Placing Files

- Input files: **flow_logs.txt** and **lookup_table.csv** in the same directory `attachments/input` under project path;
- Output report files: **tag_counts_output.csv** and **port_protocol_counts_output** will be generated in the directory `attachments/output`under project path;

## Input Files
- ### Flow Log File (flow_logs.txt)

This file contains network flow log data. Each line represents a flow log entry. The processor read two columns:
**[6] -> dstport** and **[8] -> protocol**

Example:
```
2 123456789012 eni-2d2e2f3g 192.168.2.7 77.88.55.80 49153 993 6 7 3500 1620140661 1620140721 ACCEPT OK

2 123456789012 eni-4h5i6j7k 172.16.0.2 192.0.2.146 49154 143 6 9 4500 1620140661 1620140721 ACCEPT OK
```

### 3. Build And Run
Run with the command after install maven:
```
mvn clean package
java -jar target/FlowLogProcessor-1.0-SNAPSHOT.jar
```

- ### Lookup Table File (lookup_table.csv)

This file describes mapping dstport and protocol to specific tags.

Example:
```
dstport,protocol,tag

25,tcp,sv_P1

68,udp,sv_P2
```

## Output Files
- ### Tag Count Report (tag_counts.csv)
This file summarizes occurrences of each tag matches in the log

Example:
```
Tag,Count

sv_P2,1

sv_P1,2
```

- ### Port/Protocol Combination Report (port_protocol_counts.csv)
This file summarizes occurrences of each dstport and protocol combination matches in the log

Example:
```
Port,Protocol,Count

22,tcp,1

23,tcp,1
```

## Basic Logic

Reads the flow logs and splits them among multiple threads.

Extracts the data and use Map to categorize them into different protocols and tags.

Store the data in Map and then push into the output report file.

## Multi-threading Optimization

- Splits log files into batches for parallel processing.
- Each thread processes a batch of log entries and matches tags based on looks up table.
Stores results across threads using ConcurrentHashMap.
- **Possible optimization in the future: multi-thread on reading the large log files, not load them all in memory**


