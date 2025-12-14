package com.healthdata.migration.connector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.healthdata.migration.dto.SourceConfig;

/**
 * Base class for source connectors with common functionality
 */
public abstract class AbstractSourceConnector implements SourceConnector {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected SourceConfig config;
    protected boolean connected = false;
    protected long currentPosition = 0;
    protected String currentFile = null;

    @Override
    public void connect(SourceConfig config) throws IOException {
        this.config = config;
        doConnect();
        this.connected = true;
        log.info("Connected to {} source", getType());
    }

    @Override
    public void disconnect() {
        if (connected) {
            try {
                doDisconnect();
            } catch (Exception e) {
                log.warn("Error during disconnect", e);
            }
            connected = false;
            log.info("Disconnected from {} source", getType());
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public long getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public String getCurrentFile() {
        return currentFile;
    }

    @Override
    public Map<String, Object> getCheckpointData() {
        Map<String, Object> checkpoint = new HashMap<>();
        checkpoint.put("position", currentPosition);
        checkpoint.put("currentFile", currentFile);
        addCheckpointData(checkpoint);
        return checkpoint;
    }

    @Override
    public void restoreFromCheckpoint(Map<String, Object> checkpointData) throws IOException {
        if (checkpointData == null) {
            return;
        }

        if (checkpointData.containsKey("position")) {
            this.currentPosition = ((Number) checkpointData.get("position")).longValue();
        }
        if (checkpointData.containsKey("currentFile")) {
            this.currentFile = (String) checkpointData.get("currentFile");
        }

        doRestoreFromCheckpoint(checkpointData);
        log.info("Restored from checkpoint: position={}, file={}", currentPosition, currentFile);
    }

    /**
     * Perform the actual connection. Override in subclasses.
     */
    protected abstract void doConnect() throws IOException;

    /**
     * Perform the actual disconnection. Override in subclasses.
     */
    protected abstract void doDisconnect();

    /**
     * Add connector-specific checkpoint data. Override if needed.
     */
    protected void addCheckpointData(Map<String, Object> checkpoint) {
        // Default: no additional data
    }

    /**
     * Restore connector-specific state from checkpoint. Override if needed.
     */
    protected void doRestoreFromCheckpoint(Map<String, Object> checkpointData) throws IOException {
        // Default: no additional restore needed
    }

    /**
     * Ensure connector is connected before operations
     */
    protected void ensureConnected() {
        if (!connected) {
            throw new IllegalStateException("Connector is not connected");
        }
    }
}
