package com.healthdata.migration.connector;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceRecord;
import com.healthdata.migration.dto.SourceType;

/**
 * Interface for data source connectors.
 * Implementations provide data reading from various sources (File, SFTP, MLLP).
 */
public interface SourceConnector extends AutoCloseable {

    /**
     * Connect to the data source
     *
     * @param config Configuration for the connection
     * @throws IOException If connection fails
     */
    void connect(SourceConfig config) throws IOException;

    /**
     * Disconnect from the data source
     */
    void disconnect();

    /**
     * Test the connection
     *
     * @return true if connection is successful
     */
    boolean testConnection();

    /**
     * Check if currently connected
     *
     * @return true if connected
     */
    boolean isConnected();

    /**
     * Count total records available in the source
     *
     * @return Number of records, or -1 if unknown (e.g., MLLP streaming)
     * @throws IOException If counting fails
     */
    long countRecords() throws IOException;

    /**
     * Read records in batches
     *
     * @param batchSize Number of records per batch
     * @return Iterator over source records
     * @throws IOException If reading fails
     */
    Iterator<SourceRecord> readRecords(int batchSize) throws IOException;

    /**
     * Seek to a specific position for resumability
     *
     * @param offset Position to seek to
     * @throws IOException If seeking fails
     */
    void seek(long offset) throws IOException;

    /**
     * Get current position for checkpointing
     *
     * @return Current position/offset
     */
    long getCurrentPosition();

    /**
     * Get checkpoint data for resumability
     *
     * @return Map of checkpoint data
     */
    Map<String, Object> getCheckpointData();

    /**
     * Restore from checkpoint data
     *
     * @param checkpointData Previously saved checkpoint
     * @throws IOException If restore fails
     */
    void restoreFromCheckpoint(Map<String, Object> checkpointData) throws IOException;

    /**
     * Get the source type this connector handles
     *
     * @return Source type
     */
    SourceType getType();

    /**
     * Get current file being processed (for file-based sources)
     *
     * @return Current file path, or null for non-file sources
     */
    default String getCurrentFile() {
        return null;
    }

    @Override
    default void close() throws Exception {
        disconnect();
    }
}
