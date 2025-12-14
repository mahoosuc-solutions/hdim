package com.healthdata.migration.connector;

import org.springframework.stereotype.Component;

import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;

/**
 * Factory for creating source connectors based on source type
 */
@Component
public class SourceConnectorFactory {

    /**
     * Create a connector for the given source type
     *
     * @param type Source type
     * @return Appropriate connector instance
     */
    public SourceConnector create(SourceType type) {
        return switch (type) {
            case FILE -> new FileSourceConnector();
            case SFTP -> new SftpSourceConnector();
            case MLLP -> new MllpSourceConnector();
        };
    }

    /**
     * Create and connect a connector
     *
     * @param config Source configuration
     * @return Connected connector instance
     * @throws Exception If connection fails
     */
    public SourceConnector createAndConnect(SourceConfig config) throws Exception {
        SourceConnector connector = create(config.getSourceType());
        connector.connect(config);
        return connector;
    }

    /**
     * Test connection to a source
     *
     * @param config Source configuration
     * @return true if connection test succeeds
     */
    public boolean testConnection(SourceConfig config) {
        try (SourceConnector connector = create(config.getSourceType())) {
            connector.connect(config);
            return connector.testConnection();
        } catch (Exception e) {
            return false;
        }
    }
}
