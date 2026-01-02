package com.healthdata.migration.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceRecord;
import com.healthdata.migration.dto.SourceType;

/**
 * Unit tests for FileSourceConnector
 */
@DisplayName("FileSourceConnector")
class FileSourceConnectorTest {

    @TempDir
    Path tempDir;

    private FileSourceConnector connector;

    @BeforeEach
    void setUp() {
        connector = new FileSourceConnector();
    }

    @AfterEach
    void tearDown() {
        if (connector != null && connector.isConnected()) {
            connector.disconnect();
        }
    }

    @Nested
    @DisplayName("Connection Management")
    class ConnectionTests {

        @Test
        @DisplayName("Should connect to valid file path")
        void shouldConnectToValidPath() throws IOException {
            // Given
            Path testFile = tempDir.resolve("test.hl7");
            Files.writeString(testFile, "MSH|^~\\&|TEST|||20240101000000||ADT^A01|123|P|2.5\r");

            SourceConfig config = SourceConfig.forFile(testFile.toString(), "*.hl7", false);

            // When
            connector.connect(config);

            // Then
            assertThat(connector.isConnected()).isTrue();
            assertThat(connector.testConnection()).isTrue();
        }

        @Test
        @DisplayName("Should fail to connect to non-existent path")
        void shouldFailToConnectToNonExistentPath() {
            // Given
            SourceConfig config = SourceConfig.forFile("/non/existent/path.hl7", "*.hl7", false);

            // When/Then
            assertThatThrownBy(() -> connector.connect(config))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("does not exist");
        }

        @Test
        @DisplayName("Should disconnect successfully")
        void shouldDisconnectSuccessfully() throws IOException {
            // Given
            Path testFile = tempDir.resolve("test.hl7");
            Files.writeString(testFile, "MSH|^~\\&|TEST|||20240101000000||ADT^A01|123|P|2.5\r");

            SourceConfig config = SourceConfig.forFile(testFile.toString(), "*.hl7", false);
            connector.connect(config);

            // When
            connector.disconnect();

            // Then
            assertThat(connector.isConnected()).isFalse();
        }

        @Test
        @DisplayName("Should return correct type")
        void shouldReturnCorrectType() {
            // When/Then
            assertThat(connector.getType()).isEqualTo(SourceType.FILE);
        }

        @Test
        @DisplayName("Should handle disconnect when not connected")
        void shouldHandleDisconnectWhenNotConnected() {
            // When/Then - should not throw
            connector.disconnect();
            assertThat(connector.isConnected()).isFalse();
        }
    }

    @Nested
    @DisplayName("File Pattern Matching")
    class PatternMatchingTests {

        @Test
        @DisplayName("Should find all files matching pattern in directory")
        void shouldFindMatchingFiles() throws IOException {
            // Given
            Files.writeString(tempDir.resolve("file1.hl7"), "MSH|^~\\&|TEST|||20240101000000||ADT^A01|1|P|2.5\r");
            Files.writeString(tempDir.resolve("file2.hl7"), "MSH|^~\\&|TEST|||20240101000000||ADT^A01|2|P|2.5\r");
            Files.writeString(tempDir.resolve("file3.txt"), "Not an HL7 file");

            SourceConfig config = SourceConfig.forFile(tempDir.toString() + "/*.hl7", "*.hl7", false);

            // When
            connector.connect(config);

            // Then
            assertThat(connector.testConnection()).isTrue();
        }

        @Test
        @DisplayName("Should handle recursive file search")
        void shouldHandleRecursiveSearch() throws IOException {
            // Given
            Path subDir = tempDir.resolve("subdir");
            Files.createDirectory(subDir);

            Files.writeString(tempDir.resolve("root.hl7"), "MSH|^~\\&|TEST|||20240101000000||ADT^A01|1|P|2.5\r");
            Files.writeString(subDir.resolve("nested.hl7"), "MSH|^~\\&|TEST|||20240101000000||ADT^A01|2|P|2.5\r");

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(tempDir.toString() + "/*.hl7")
                    .filePattern("*.hl7")
                    .recursive(true)
                    .build();

            // When
            connector.connect(config);

            // Then
            assertThat(connector.testConnection()).isTrue();
        }

        @Test
        @DisplayName("Should handle single file path")
        void shouldHandleSingleFile() throws IOException {
            // Given
            Path testFile = tempDir.resolve("single.hl7");
            Files.writeString(testFile, "MSH|^~\\&|TEST|||20240101000000||ADT^A01|123|P|2.5\r");

            SourceConfig config = SourceConfig.forFile(testFile.toString(), null, false);

            // When
            connector.connect(config);

            // Then
            assertThat(connector.testConnection()).isTrue();
        }
    }

    @Nested
    @DisplayName("Record Reading")
    class RecordReadingTests {

        @Test
        @DisplayName("Should read single HL7 message from file")
        void shouldReadSingleHl7Message() throws IOException {
            // Given
            Path testFile = tempDir.resolve("test.hl7");
            String hl7Message = "MSH|^~\\&|TEST|||20240101000000||ADT^A01|123|P|2.5\r" +
                    "PID|1||12345^^^MRN||DOE^JOHN||19800101|M\r";
            Files.writeString(testFile, hl7Message);

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(testFile.toString())
                    .dataType(DataType.HL7V2)
                    .build();

            connector.connect(config);

            // When
            Iterator<SourceRecord> iterator = connector.readRecords(10);
            List<SourceRecord> records = new ArrayList<>();
            iterator.forEachRemaining(records::add);

            // Then
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getContent()).contains("MSH");
            assertThat(records.get(0).getDataType()).isEqualTo(DataType.HL7V2);
            assertThat(records.get(0).getSourceFile()).isEqualTo(testFile.toString());
        }

        @Test
        @DisplayName("Should read multiple HL7 messages from file")
        void shouldReadMultipleHl7Messages() throws IOException {
            // Given
            Path testFile = tempDir.resolve("multiple.hl7");
            String messages = "MSH|^~\\&|TEST|||20240101000000||ADT^A01|1|P|2.5\r" +
                    "PID|1||12345^^^MRN||DOE^JOHN||19800101|M\r" +
                    "MSH|^~\\&|TEST|||20240101000000||ADT^A01|2|P|2.5\r" +
                    "PID|1||67890^^^MRN||SMITH^JANE||19900101|F\r";
            Files.writeString(testFile, messages);

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(testFile.toString())
                    .dataType(DataType.HL7V2)
                    .build();

            connector.connect(config);

            // When
            Iterator<SourceRecord> iterator = connector.readRecords(10);
            List<SourceRecord> records = new ArrayList<>();
            iterator.forEachRemaining(records::add);

            // Then
            assertThat(records).hasSize(2);
            assertThat(records.get(0).getContent()).contains("12345");
            assertThat(records.get(1).getContent()).contains("67890");
        }

        @Test
        @DisplayName("Should throw exception when reading without connection")
        void shouldThrowWhenReadingWithoutConnection() {
            // When/Then
            assertThatThrownBy(() -> connector.readRecords(10))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not connected");
        }

        @Test
        @DisplayName("Should handle empty file")
        void shouldHandleEmptyFile() throws IOException {
            // Given
            Path testFile = tempDir.resolve("empty.hl7");
            Files.writeString(testFile, "");

            SourceConfig config = SourceConfig.forFile(testFile.toString(), "*.hl7", false);
            connector.connect(config);

            // When
            Iterator<SourceRecord> iterator = connector.readRecords(10);
            List<SourceRecord> records = new ArrayList<>();
            iterator.forEachRemaining(records::add);

            // Then
            assertThat(records).isEmpty();
        }

        @Test
        @DisplayName("Should read gzip-compressed HL7 file")
        void shouldReadGzipFile() throws IOException {
            Path testFile = tempDir.resolve("compressed.hl7.gz");
            String message = "MSH|^~\\&|TEST|||20240101000000||ADT^A01|123|P|2.5\r";
            try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(testFile))) {
                gzip.write(message.getBytes(StandardCharsets.UTF_8));
            }

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(testFile.toString())
                    .dataType(DataType.HL7V2)
                    .compression(SourceConfig.CompressionType.GZIP)
                    .build();

            connector.connect(config);

            Iterator<SourceRecord> iterator = connector.readRecords(10);
            assertThat(iterator.hasNext()).isTrue();
            SourceRecord record = iterator.next();
            assertThat(record.getContent()).contains("MSH");
        }

        @Test
        @DisplayName("Should detect JSON bundle when data type is not set")
        void shouldDetectJsonBundle() throws IOException {
            Path testFile = tempDir.resolve("bundle.json");
            Files.writeString(testFile, "{\"resourceType\":\"Bundle\"}");

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(testFile.toString())
                    .build();

            connector.connect(config);

            Iterator<SourceRecord> iterator = connector.readRecords(10);
            SourceRecord record = iterator.next();
            assertThat(record.getDataType()).isEqualTo(DataType.FHIR_BUNDLE);
        }
    }

    @Nested
    @DisplayName("Record Counting")
    class RecordCountingTests {

        @Test
        @DisplayName("Should count HL7 messages correctly")
        void shouldCountHl7Messages() throws IOException {
            // Given
            Path testFile = tempDir.resolve("count.hl7");
            String messages = "MSH|^~\\&|TEST|||20240101000000||ADT^A01|1|P|2.5\r" +
                    "PID|1||12345^^^MRN||DOE^JOHN||19800101|M\r" +
                    "MSH|^~\\&|TEST|||20240101000000||ADT^A01|2|P|2.5\r" +
                    "PID|1||67890^^^MRN||SMITH^JANE||19900101|F\r" +
                    "MSH|^~\\&|TEST|||20240101000000||ADT^A01|3|P|2.5\r" +
                    "PID|1||11111^^^MRN||BROWN^BOB||19700101|M\r";
            Files.writeString(testFile, messages);

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(testFile.toString())
                    .dataType(DataType.HL7V2)
                    .build();

            connector.connect(config);

            // When
            long count = connector.countRecords();

            // Then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should count CDA documents as one per file")
        void shouldCountCdaDocuments() throws IOException {
            // Given
            Path file1 = tempDir.resolve("doc1.xml");
            Path file2 = tempDir.resolve("doc2.xml");
            Files.writeString(file1, "<ClinicalDocument>...</ClinicalDocument>");
            Files.writeString(file2, "<ClinicalDocument>...</ClinicalDocument>");

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(tempDir.toString() + "/*.xml")
                    .dataType(DataType.CDA)
                    .recursive(false)
                    .build();

            connector.connect(config);

            // When
            long count = connector.countRecords();

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count files when data type is not specified")
        void shouldCountFilesWhenDataTypeNotSpecified() throws IOException {
            Path file1 = tempDir.resolve("file1.hl7");
            Path file2 = tempDir.resolve("file2.hl7");
            Files.writeString(file1, "MSH|^~\\&|TEST");
            Files.writeString(file2, "MSH|^~\\&|TEST");

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(tempDir.toString() + "/*.hl7")
                    .build();

            connector.connect(config);

            long count = connector.countRecords();

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should throw when counting without connection")
        void shouldThrowWhenCountingWithoutConnection() {
            // When/Then
            assertThatThrownBy(() -> connector.countRecords())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not connected");
        }
    }

    @Nested
    @DisplayName("Checkpoint and Resume")
    class CheckpointTests {

        @Test
        @DisplayName("Should track current position")
        void shouldTrackCurrentPosition() throws IOException {
            // Given
            Path testFile = tempDir.resolve("position.hl7");
            String messages = "MSH|^~\\&|TEST|||20240101000000||ADT^A01|1|P|2.5\r" +
                    "PID|1||12345^^^MRN||DOE^JOHN||19800101|M\r" +
                    "MSH|^~\\&|TEST|||20240101000000||ADT^A01|2|P|2.5\r" +
                    "PID|1||67890^^^MRN||SMITH^JANE||19900101|F\r";
            Files.writeString(testFile, messages);

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(testFile.toString())
                    .dataType(DataType.HL7V2)
                    .build();

            connector.connect(config);

            // When
            Iterator<SourceRecord> iterator = connector.readRecords(10);
            iterator.next(); // Read first record

            // Then
            assertThat(connector.getCurrentPosition()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should save checkpoint data")
        void shouldSaveCheckpointData() throws IOException {
            // Given
            Path testFile = tempDir.resolve("checkpoint.hl7");
            Files.writeString(testFile, "MSH|^~\\&|TEST|||20240101000000||ADT^A01|1|P|2.5\r");

            SourceConfig config = SourceConfig.forFile(testFile.toString(), "*.hl7", false);
            connector.connect(config);

            // When
            Map<String, Object> checkpoint = connector.getCheckpointData();

            // Then
            assertThat(checkpoint).isNotNull();
            assertThat(checkpoint).containsKeys("position", "currentFile");
        }

        @Test
        @DisplayName("Should restore from checkpoint")
        void shouldRestoreFromCheckpoint() throws IOException {
            // Given
            Path testFile = tempDir.resolve("restore.hl7");
            Files.writeString(testFile, "MSH|^~\\&|TEST|||20240101000000||ADT^A01|1|P|2.5\r");

            SourceConfig config = SourceConfig.forFile(testFile.toString(), "*.hl7", false);
            connector.connect(config);

            Map<String, Object> checkpoint = Map.of(
                    "position", 5L,
                    "currentFile", testFile.toString(),
                    "fileIndex", 0,
                    "recordsRead", 5L
            );

            // When
            connector.restoreFromCheckpoint(checkpoint);

            // Then
            assertThat(connector.getCurrentPosition()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should handle null checkpoint gracefully")
        void shouldHandleNullCheckpoint() throws IOException {
            // Given
            Path testFile = tempDir.resolve("nullcheck.hl7");
            Files.writeString(testFile, "MSH|^~\\&|TEST|||20240101000000||ADT^A01|1|P|2.5\r");

            SourceConfig config = SourceConfig.forFile(testFile.toString(), "*.hl7", false);
            connector.connect(config);

            // When/Then - should not throw
            connector.restoreFromCheckpoint(null);
        }
    }

    @Nested
    @DisplayName("Data Type Detection")
    class DataTypeDetectionTests {

        @Test
        @DisplayName("Should auto-detect HL7 from .hl7 extension")
        void shouldDetectHl7Extension() throws IOException {
            // Given
            Path testFile = tempDir.resolve("auto.hl7");
            Files.writeString(testFile, "MSH|^~\\&|TEST|||20240101000000||ADT^A01|1|P|2.5\r");

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(testFile.toString())
                    .build(); // No dataType specified

            connector.connect(config);

            // When
            Iterator<SourceRecord> iterator = connector.readRecords(10);
            List<SourceRecord> records = new ArrayList<>();
            iterator.forEachRemaining(records::add);

            // Then
            assertThat(records).isNotEmpty();
            // Should default to HL7V2 based on extension
        }

        @Test
        @DisplayName("Should read XML file as CDA")
        void shouldReadXmlAsCda() throws IOException {
            // Given
            Path testFile = tempDir.resolve("document.xml");
            Files.writeString(testFile, "<ClinicalDocument>Content</ClinicalDocument>");

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(testFile.toString())
                    .dataType(DataType.CDA)
                    .build();

            connector.connect(config);

            // When
            Iterator<SourceRecord> iterator = connector.readRecords(10);
            List<SourceRecord> records = new ArrayList<>();
            iterator.forEachRemaining(records::add);

            // Then
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getContent()).contains("ClinicalDocument");
            assertThat(records.get(0).getDataType()).isEqualTo(DataType.CDA);
        }
    }

    @Nested
    @DisplayName("Seek Operations")
    class SeekTests {

        @Test
        @DisplayName("Should seek to specific position")
        void shouldSeekToPosition() throws IOException {
            // Given
            Path testFile = tempDir.resolve("seek.hl7");
            String messages = "MSH|^~\\&|TEST|||20240101000000||ADT^A01|1|P|2.5\r" +
                    "MSH|^~\\&|TEST|||20240101000000||ADT^A01|2|P|2.5\r" +
                    "MSH|^~\\&|TEST|||20240101000000||ADT^A01|3|P|2.5\r";
            Files.writeString(testFile, messages);

            SourceConfig config = SourceConfig.builder()
                    .sourceType(SourceType.FILE)
                    .path(testFile.toString())
                    .dataType(DataType.HL7V2)
                    .build();

            connector.connect(config);

            // When
            connector.seek(2);

            // Then
            assertThat(connector.getCurrentPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should throw when seeking without connection")
        void shouldThrowWhenSeekingWithoutConnection() {
            // When/Then
            assertThatThrownBy(() -> connector.seek(5))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not connected");
        }
    }
}
