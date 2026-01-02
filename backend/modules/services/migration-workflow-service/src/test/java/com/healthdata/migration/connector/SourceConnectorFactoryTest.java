package com.healthdata.migration.connector;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;

@DisplayName("SourceConnectorFactory")
class SourceConnectorFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should create connector types")
    void shouldCreateConnectorTypes() {
        SourceConnectorFactory factory = new SourceConnectorFactory();

        assertThat(factory.create(SourceType.FILE)).isInstanceOf(FileSourceConnector.class);
        assertThat(factory.create(SourceType.SFTP)).isInstanceOf(SftpSourceConnector.class);
        assertThat(factory.create(SourceType.MLLP)).isInstanceOf(MllpSourceConnector.class);
    }

    @Test
    @DisplayName("Should create and connect file connector")
    void shouldCreateAndConnectFileConnector() throws Exception {
        Path file = tempDir.resolve("test.hl7");
        Files.writeString(file, "MSH|^~\\&|");

        SourceConfig config = SourceConfig.forFile(file.toString(), "*.hl7", false);
        SourceConnectorFactory factory = new SourceConnectorFactory();

        SourceConnector connector = factory.createAndConnect(config);
        try {
            assertThat(connector.isConnected()).isTrue();
            assertThat(connector.testConnection()).isTrue();
        } finally {
            connector.disconnect();
        }
    }

    @Test
    @DisplayName("Should return false when connection fails")
    void shouldReturnFalseWhenConnectionFails() {
        SourceConfig config = SourceConfig.forFile("/missing/path.hl7", "*.hl7", false);
        SourceConnectorFactory factory = new SourceConnectorFactory();

        assertThat(factory.testConnection(config)).isFalse();
    }

    @Test
    @DisplayName("Should return true when connection succeeds")
    void shouldReturnTrueWhenConnectionSucceeds() throws Exception {
        Path file = tempDir.resolve("ok.hl7");
        Files.writeString(file, "MSH|^~\\&|");

        SourceConfig config = SourceConfig.forFile(file.toString(), "*.hl7", false);
        SourceConnectorFactory factory = new SourceConnectorFactory();

        assertThat(factory.testConnection(config)).isTrue();
    }

    @Test
    @DisplayName("Should close default connector")
    void shouldCloseDefaultConnector() throws Exception {
        java.util.concurrent.atomic.AtomicBoolean disconnected = new java.util.concurrent.atomic.AtomicBoolean(false);
        SourceConnector connector = new SourceConnector() {
            @Override
            public void connect(SourceConfig config) throws IOException {
                // no-op
            }

            @Override
            public void disconnect() {
                disconnected.set(true);
            }

            @Override
            public boolean testConnection() {
                return true;
            }

            @Override
            public boolean isConnected() {
                return true;
            }

            @Override
            public long countRecords() {
                return 0;
            }

            @Override
            public java.util.Iterator<com.healthdata.migration.dto.SourceRecord> readRecords(int batchSize) {
                return java.util.List.<com.healthdata.migration.dto.SourceRecord>of().iterator();
            }

            @Override
            public void seek(long offset) {
                // no-op
            }

            @Override
            public long getCurrentPosition() {
                return 0;
            }

            @Override
            public java.util.Map<String, Object> getCheckpointData() {
                return java.util.Map.of();
            }

            @Override
            public void restoreFromCheckpoint(java.util.Map<String, Object> checkpointData) {
                // no-op
            }

            @Override
            public SourceType getType() {
                return SourceType.FILE;
            }
        };

        assertThat(connector.getCurrentFile()).isNull();
        connector.close();
        assertThat(disconnected.get()).isTrue();
    }
}
