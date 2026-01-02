package com.healthdata.cms.client;

import com.healthdata.cms.auth.OAuth2Manager;
import com.healthdata.cms.model.CmsApiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * BCDA (Beneficiary Claims Data API) Client
 * 
 * Handles integration with CMS BCDA API for bulk Medicare claims exports.
 * 
 * Features:
 * - Weekly bulk export of Part A, B, D claims
 * - NDJSON format (newline-delimited JSON)
 * - OAuth2 authentication
 * - Batch polling for export status
 * 
 * Documentation: https://bcda.cms.gov/api/v2
 */
@Slf4j
@Component
public class BcdaClient {

    private final OAuth2Manager oauth2Manager;
    private final String baseUrl;
    private final boolean isSandbox;

    public BcdaClient(OAuth2Manager oauth2Manager, 
                      @Value("${cms.bcda.base-url:https://sandbox.bcda.cms.gov}") String baseUrl,
                      @Value("${cms.sandbox-mode:true}") boolean isSandbox) {
        this.oauth2Manager = oauth2Manager;
        this.baseUrl = baseUrl;
        this.isSandbox = isSandbox;
    }

    /**
     * Get the authorization header with OAuth2 token
     */
    private String getAuthorizationHeader() {
        String token = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);
        return "Bearer " + token;
    }

    /**
     * List available bulk data exports
     * GET /api/v2/bulkdata
     */
    public List<BulkDataExport> listBulkDataExports() {
        log.info("Fetching list of BCDA bulk data exports");
        
        // TODO: Week 2 - Implement Feign client or RestTemplate call
        // For now, return empty list
        return List.of();
    }

    /**
     * Request a new bulk data export
     * POST /api/v2/bulkdata
     */
    public BulkDataExportResponse requestBulkDataExport(BulkDataExportRequest request) {
        log.info("Requesting BCDA bulk data export with output format: {}", request.getOutputFormat());
        
        // TODO: Week 2 - Implement export request
        return new BulkDataExportResponse("pending", "export-123");
    }

    /**
     * Get status of a bulk data export
     * GET /api/v2/bulkdata/{exportId}
     */
    public BulkDataExportStatus getExportStatus(String exportId) {
        log.info("Fetching status for BCDA export: {}", exportId);
        
        // TODO: Week 2 - Implement status polling
        return new BulkDataExportStatus("pending", 0);
    }

    /**
     * Get download URLs for completed export files
     * GET /api/v2/bulkdata/{exportId}/file_paths
     */
    public List<String> getExportFilePaths(String exportId) {
        log.info("Fetching file paths for BCDA export: {}", exportId);
        
        // TODO: Week 2 - Implement file path retrieval
        return List.of();
    }

    /**
     * Download bulk data export file
     * GET /api/v2/File/{fileName}
     */
    public String downloadFile(String fileName) {
        log.info("Downloading BCDA file: {}", fileName);
        
        // TODO: Week 2 - Implement file download
        return "";
    }

    /**
     * Get BCDA metadata
     * GET /api/v2
     */
    public BcdaMetadata getMetadata() {
        log.info("Fetching BCDA API metadata");
        
        // TODO: Week 2 - Implement metadata retrieval
        return new BcdaMetadata();
    }

    // ============ DTOs ============

    public static class BulkDataExport {
        private String id;
        private String status;
        private String createdAt;
        private List<String> filePaths;

        public BulkDataExport() {}

        public String getId() { return id; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
        public List<String> getFilePaths() { return filePaths; }
    }

    public static class BulkDataExportRequest {
        private String outputFormat; // "ndjson"
        private List<String> resourceTypes; // ["ExplanationOfBenefit", "Patient", "Coverage"]

        public BulkDataExportRequest() {}

        public String getOutputFormat() { return outputFormat; }
        public List<String> getResourceTypes() { return resourceTypes; }
    }

    public static class BulkDataExportResponse {
        private String status;
        private String exportId;

        public BulkDataExportResponse(String status, String exportId) {
            this.status = status;
            this.exportId = exportId;
        }

        public String getStatus() { return status; }
        public String getExportId() { return exportId; }
    }

    public static class BulkDataExportStatus {
        private String status;
        private int percentComplete;

        public BulkDataExportStatus(String status, int percentComplete) {
            this.status = status;
            this.percentComplete = percentComplete;
        }

        public String getStatus() { return status; }
        public int getPercentComplete() { return percentComplete; }
    }

    public static class BcdaMetadata {
        private String apiVersion;
        private List<String> supportedResourceTypes;
        private String dataLag; // "5-10 days"

        public BcdaMetadata() {}

        public String getApiVersion() { return apiVersion; }
        public List<String> getSupportedResourceTypes() { return supportedResourceTypes; }
        public String getDataLag() { return dataLag; }
    }
}
