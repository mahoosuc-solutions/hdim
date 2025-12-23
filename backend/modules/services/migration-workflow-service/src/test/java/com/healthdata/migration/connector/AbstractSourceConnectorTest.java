package com.healthdata.migration.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;

@DisplayName("AbstractSourceConnector")
class AbstractSourceConnectorTest {

    @Test
    @DisplayName("Should restore checkpoint fields and delegate to subclass")
    void shouldRestoreCheckpointFields() throws IOException {
        TestConnector connector = new TestConnector();
        Map<String, Object> checkpoint = Map.of(
                "position", 5L,
                "currentFile", "file.hl7",
                "extra", "value"
        );

        connector.restoreFromCheckpoint(checkpoint);

        assertThat(connector.getCurrentPosition()).isEqualTo(5L);
        assertThat(connector.getCurrentFile()).isEqualTo("file.hl7");
        assertThat(connector.restored).containsEntry("extra", "value");
    }

    @Test
    @DisplayName("Should ignore null checkpoint data")
    void shouldIgnoreNullCheckpoint() throws IOException {
        TestConnector connector = new TestConnector();
        assertThatCode(() -> connector.restoreFromCheckpoint(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should swallow disconnect exceptions")
    void shouldSwallowDisconnectExceptions() throws IOException {
        TestConnector connector = new TestConnector();
        connector.throwOnDisconnect = true;

        connector.connect(SourceConfig.forFile("file.hl7", "*.hl7", false));

        assertThatCode(connector::disconnect).doesNotThrowAnyException();
        assertThat(connector.isConnected()).isFalse();
    }

    private static class TestConnector extends AbstractSourceConnector {
        private Map<String, Object> restored;
        private boolean throwOnDisconnect;

        @Override
        protected void doConnect() {
            // no-op
        }

        @Override
        protected void doDisconnect() {
            if (throwOnDisconnect) {
                throw new RuntimeException("disconnect failed");
            }
        }

        @Override
        public boolean testConnection() {
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
        protected void addCheckpointData(Map<String, Object> checkpoint) {
            checkpoint.put("extra", "value");
        }

        @Override
        protected void doRestoreFromCheckpoint(Map<String, Object> checkpointData) {
            restored = checkpointData;
        }

        @Override
        public SourceType getType() {
            return SourceType.FILE;
        }
    }
}
