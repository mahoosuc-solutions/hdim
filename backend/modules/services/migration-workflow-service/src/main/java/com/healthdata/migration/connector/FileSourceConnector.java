package com.healthdata.migration.connector;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceRecord;
import com.healthdata.migration.dto.SourceType;

/**
 * Source connector for reading files from local filesystem or NFS.
 * Supports single files, directories with glob patterns, and gzipped files.
 */
public class FileSourceConnector extends AbstractSourceConnector {

    private List<Path> filesToProcess;
    private int currentFileIndex = 0;
    private long recordsRead = 0;
    private BufferedReader currentReader;

    @Override
    public SourceType getType() {
        return SourceType.FILE;
    }

    @Override
    protected void doConnect() throws IOException {
        String pathPattern = config.getPath();
        Path basePath = Paths.get(pathPattern).getParent();
        String globPattern = Paths.get(pathPattern).getFileName().toString();

        if (basePath == null) {
            basePath = Paths.get(".");
        }

        // Collect all matching files
        filesToProcess = new ArrayList<>();

        if (Files.isRegularFile(Paths.get(pathPattern))) {
            // Single file
            filesToProcess.add(Paths.get(pathPattern));
        } else if (Files.isDirectory(basePath)) {
            // Directory with pattern
            collectFiles(basePath, globPattern, config.isRecursive());
        } else {
            throw new IOException("Path does not exist: " + pathPattern);
        }

        // Sort files for deterministic ordering
        Collections.sort(filesToProcess);

        log.info("Found {} files to process", filesToProcess.size());
    }

    private void collectFiles(Path baseDir, String pattern, boolean recursive) throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        if (recursive) {
            Files.walkFileTree(baseDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (matcher.matches(file.getFileName())) {
                        filesToProcess.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            try (var stream = Files.list(baseDir)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> matcher.matches(p.getFileName()))
                      .forEach(filesToProcess::add);
            }
        }
    }

    @Override
    protected void doDisconnect() {
        closeCurrentReader();
    }

    @Override
    public boolean testConnection() {
        return filesToProcess != null && !filesToProcess.isEmpty();
    }

    @Override
    public long countRecords() throws IOException {
        ensureConnected();

        // For file sources, we count based on file type
        // HL7 messages are delimited, CDA/FHIR are one per file
        if (config.getDataType() == null) {
            return filesToProcess.size();
        }

        return switch (config.getDataType()) {
            case CDA, FHIR_BUNDLE -> filesToProcess.size(); // One document per file
            case HL7V2 -> countHl7Messages(); // Multiple messages possible per file
        };
    }

    private long countHl7Messages() throws IOException {
        long count = 0;
        for (Path file : filesToProcess) {
            count += countHl7MessagesInFile(file);
        }
        return count;
    }

    private long countHl7MessagesInFile(Path file) throws IOException {
        long count = 0;
        try (BufferedReader reader = createReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MSH")) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public Iterator<SourceRecord> readRecords(int batchSize) throws IOException {
        ensureConnected();
        return new FileRecordIterator(batchSize);
    }

    @Override
    public void seek(long offset) throws IOException {
        ensureConnected();

        // For file-based sources, offset represents record number
        // We need to skip that many records
        recordsRead = 0;
        currentFileIndex = 0;
        closeCurrentReader();

        while (recordsRead < offset && currentFileIndex < filesToProcess.size()) {
            // Skip records until we reach the offset
            Iterator<SourceRecord> iter = readRecords(100);
            while (iter.hasNext() && recordsRead < offset) {
                iter.next();
                recordsRead++;
            }
        }
        currentPosition = recordsRead;
        log.info("Seeked to position {}", currentPosition);
    }

    @Override
    protected void addCheckpointData(Map<String, Object> checkpoint) {
        checkpoint.put("fileIndex", currentFileIndex);
        checkpoint.put("recordsRead", recordsRead);
    }

    @Override
    protected void doRestoreFromCheckpoint(Map<String, Object> checkpointData) throws IOException {
        if (checkpointData.containsKey("fileIndex")) {
            currentFileIndex = ((Number) checkpointData.get("fileIndex")).intValue();
        }
        if (checkpointData.containsKey("recordsRead")) {
            recordsRead = ((Number) checkpointData.get("recordsRead")).longValue();
            currentPosition = recordsRead;
        }
    }

    private BufferedReader createReader(Path file) throws IOException {
        InputStream is = new FileInputStream(file.toFile());

        // Handle gzip compression
        if (config.getCompression() == SourceConfig.CompressionType.GZIP ||
            file.toString().endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }

        String encoding = config.getEncoding() != null ? config.getEncoding() : "UTF-8";
        return new BufferedReader(new InputStreamReader(is, Charset.forName(encoding)));
    }

    private void closeCurrentReader() {
        if (currentReader != null) {
            try {
                currentReader.close();
            } catch (IOException e) {
                log.warn("Error closing reader", e);
            }
            currentReader = null;
        }
    }

    /**
     * Iterator that reads records across multiple files
     */
    private class FileRecordIterator implements Iterator<SourceRecord> {
        private final int batchSize;
        private SourceRecord nextRecord;
        private StringBuilder messageBuffer = new StringBuilder();
        private boolean inMessage = false;

        FileRecordIterator(int batchSize) {
            this.batchSize = batchSize;
        }

        @Override
        public boolean hasNext() {
            if (nextRecord != null) {
                return true;
            }
            try {
                nextRecord = readNextRecord();
                return nextRecord != null;
            } catch (IOException e) {
                throw new RuntimeException("Error reading next record", e);
            }
        }

        @Override
        public SourceRecord next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            SourceRecord record = nextRecord;
            nextRecord = null;
            recordsRead++;
            currentPosition = recordsRead;
            return record;
        }

        private SourceRecord readNextRecord() throws IOException {
            while (currentFileIndex < filesToProcess.size()) {
                Path currentPath = filesToProcess.get(currentFileIndex);
                currentFile = currentPath.toString();

                if (currentReader == null) {
                    currentReader = createReader(currentPath);
                }

                SourceRecord record = readRecordFromCurrentFile();
                if (record != null) {
                    return record;
                }

                // Move to next file
                closeCurrentReader();
                currentFileIndex++;
            }

            return null;
        }

        private SourceRecord readRecordFromCurrentFile() throws IOException {
            DataType dataType = detectDataType();

            return switch (dataType) {
                case HL7V2 -> readHl7Record();
                case CDA, FHIR_BUNDLE -> readWholeFileRecord(dataType);
            };
        }

        private DataType detectDataType() {
            if (config.getDataType() != null) {
                return config.getDataType();
            }

            // Try to detect from file extension
            String fileName = currentFile.toLowerCase();
            if (fileName.endsWith(".hl7")) {
                return DataType.HL7V2;
            } else if (fileName.endsWith(".xml")) {
                return DataType.CDA;
            } else if (fileName.endsWith(".json")) {
                return DataType.FHIR_BUNDLE;
            }

            // Default to HL7
            return DataType.HL7V2;
        }

        private SourceRecord readHl7Record() throws IOException {
            String line;
            messageBuffer.setLength(0);

            while ((line = currentReader.readLine()) != null) {
                // HL7 messages start with MSH segment
                if (line.startsWith("MSH")) {
                    if (messageBuffer.length() > 0) {
                        // We have a complete previous message
                        String message = messageBuffer.toString();
                        messageBuffer.setLength(0);
                        messageBuffer.append(line).append("\r");
                        return SourceRecord.of(message, DataType.HL7V2, currentFile, recordsRead);
                    }
                    messageBuffer.append(line).append("\r");
                    inMessage = true;
                } else if (inMessage && !line.trim().isEmpty()) {
                    messageBuffer.append(line).append("\r");
                }
            }

            // End of file - return any remaining message
            if (messageBuffer.length() > 0) {
                String message = messageBuffer.toString();
                messageBuffer.setLength(0);
                inMessage = false;
                return SourceRecord.of(message, DataType.HL7V2, currentFile, recordsRead);
            }

            return null;
        }

        private SourceRecord readWholeFileRecord(DataType dataType) throws IOException {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = currentReader.readLine()) != null) {
                content.append(line).append("\n");
            }

            if (content.length() > 0) {
                return SourceRecord.of(content.toString(), dataType, currentFile, recordsRead);
            }
            return null;
        }
    }
}
