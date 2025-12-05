package com.healthdata.fhir.bulk;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for FHIR Bulk Data Export
 *
 * Provides configuration for:
 * - Export directory path
 * - Maximum concurrent exports
 * - Chunk size for large exports
 * - Export file retention days
 */
@Configuration
@ConfigurationProperties(prefix = "fhir.bulk-export")
@Data
public class BulkExportConfig {

    /**
     * Directory path where export files are stored
     * Default: /tmp/fhir-exports
     */
    private String exportDirectory = "/tmp/fhir-exports";

    /**
     * Maximum number of concurrent export jobs
     * Default: 5
     */
    private int maxConcurrentExports = 5;

    /**
     * Number of records to process in each chunk
     * Default: 1000
     */
    private int chunkSize = 1000;

    /**
     * Number of days to retain export files before cleanup
     * Default: 7 days
     */
    private int retentionDays = 7;

    /**
     * Base URL for download links
     * Default: http://localhost:8085/fhir
     */
    private String baseUrl = "http://localhost:8085/fhir";

    /**
     * Enable access token requirement for downloads
     * Default: true
     */
    private boolean requireAccessToken = true;

    /**
     * Async executor thread pool size
     * Default: 3
     */
    private int asyncExecutorPoolSize = 3;
}
