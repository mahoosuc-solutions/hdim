package com.healthdata.cms.client;

import com.healthdata.cms.auth.OAuth2Manager;
import com.healthdata.cms.exception.CmsApiException;
import com.healthdata.cms.model.CmsApiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

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

    private static final String API_VERSION = "/api/v2";
    private static final String FHIR_CONTENT_TYPE = "application/fhir+json";
    private static final String NDJSON_CONTENT_TYPE = "application/fhir+ndjson";

    private final OAuth2Manager oauth2Manager;
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final boolean isSandbox;

    public BcdaClient(OAuth2Manager oauth2Manager,
                      @Qualifier("cmsRestTemplate") RestTemplate restTemplate,
                      @Value("${cms.bcda.base-url:https://sandbox.bcda.cms.gov}") String baseUrl,
                      @Value("${cms.sandbox-mode:true}") boolean isSandbox) {
        this.oauth2Manager = oauth2Manager;
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.isSandbox = isSandbox;
        log.info("BCDA Client initialized with base URL: {} (sandbox: {})", baseUrl, isSandbox);
    }

    /**
     * Create headers with OAuth2 authentication
     */
    private HttpHeaders createHeaders() {
        String token = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", FHIR_CONTENT_TYPE);
        headers.set("Prefer", "respond-async");
        return headers;
    }

    /**
     * Create headers for file download (NDJSON content)
     */
    private HttpHeaders createDownloadHeaders() {
        String token = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", NDJSON_CONTENT_TYPE);
        return headers;
    }

    /**
     * Initiate a bulk data export for Patient resources
     * GET /api/v2/Patient/$export
     *
     * @param since Optional: Only export records updated after this date (FHIR instant format)
     * @return Export job URL from Content-Location header
     * @throws CmsApiException if export request fails
     */
    public BulkDataExportResponse requestPatientExport(String since) {
        log.info("Requesting BCDA Patient bulk export (since: {})", since);
        return requestBulkExport("/Patient/$export", since);
    }

    /**
     * Initiate a bulk data export for all Group resources
     * GET /api/v2/Group/all/$export
     *
     * @param since Optional: Only export records updated after this date
     * @return Export job URL from Content-Location header
     * @throws CmsApiException if export request fails
     */
    public BulkDataExportResponse requestGroupExport(String since) {
        log.info("Requesting BCDA Group bulk export (since: {})", since);
        return requestBulkExport("/Group/all/$export", since);
    }

    /**
     * Initiate a bulk data export with specific resource types
     * GET /api/v2/Group/all/$export?_type=ExplanationOfBenefit,Patient,Coverage
     *
     * @param resourceTypes List of FHIR resource types to export
     * @param since Optional: Only export records updated after this date
     * @return Export job URL from Content-Location header
     * @throws CmsApiException if export request fails
     */
    public BulkDataExportResponse requestBulkExport(List<String> resourceTypes, String since) {
        log.info("Requesting BCDA bulk export for types: {} (since: {})", resourceTypes, since);

        StringBuilder urlBuilder = new StringBuilder(baseUrl)
            .append(API_VERSION)
            .append("/Group/all/$export");

        List<String> params = new ArrayList<>();
        if (resourceTypes != null && !resourceTypes.isEmpty()) {
            params.add("_type=" + String.join(",", resourceTypes));
        }
        if (since != null && !since.isEmpty()) {
            params.add("_since=" + since);
        }

        if (!params.isEmpty()) {
            urlBuilder.append("?").append(String.join("&", params));
        }

        return executeExportRequest(urlBuilder.toString());
    }

    /**
     * Get the status of a bulk data export job
     * GET {jobUrl}
     *
     * @param jobUrl The Content-Location URL returned from export request
     * @return Export status with progress and output files
     * @throws CmsApiException if status check fails
     */
    public BulkDataExportStatus getExportStatus(String jobUrl) {
        log.info("Checking BCDA export status: {}", jobUrl);

        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                jobUrl, HttpMethod.GET, request, String.class
            );

            HttpStatusCode statusCode = response.getStatusCode();

            // 202 Accepted = Still processing
            if (statusCode.value() == 202) {
                String progress = response.getHeaders().getFirst("X-Progress");
                int percentComplete = parseProgress(progress);
                log.info("Export in progress: {}%", percentComplete);
                return new BulkDataExportStatus("in-progress", percentComplete, null, null);
            }

            // 200 OK = Complete
            if (statusCode.is2xxSuccessful() && response.getBody() != null) {
                log.info("Export complete, parsing output files");
                return parseExportStatusResponse(response.getBody());
            }

            throw new CmsApiException("Unexpected status from BCDA export: " + statusCode);

        } catch (HttpClientErrorException e) {
            // 404 = Job not found or expired
            if (e.getStatusCode().value() == 404) {
                return new BulkDataExportStatus("expired", 0, null, "Export job not found or expired");
            }
            throw new CmsApiException("BCDA export status error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);

        } catch (HttpServerErrorException e) {
            throw new CmsApiException("BCDA server error checking export status: " + e.getStatusCode(), e);

        } catch (RestClientException e) {
            throw new CmsApiException("Network error checking BCDA export status: " + e.getMessage(), e);
        }
    }

    /**
     * Download a bulk export file
     * GET {fileUrl}
     *
     * @param fileUrl The URL of the file to download (from export status response)
     * @return NDJSON content as string
     * @throws CmsApiException if download fails
     */
    public String downloadExportFile(String fileUrl) {
        log.info("Downloading BCDA export file: {}", fileUrl);

        try {
            HttpEntity<Void> request = new HttpEntity<>(createDownloadHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                fileUrl, HttpMethod.GET, request, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully downloaded file ({} bytes)", response.getBody().length());
                return response.getBody();
            }

            throw new CmsApiException("Empty response downloading BCDA file");

        } catch (HttpClientErrorException e) {
            throw new CmsApiException("BCDA download error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);

        } catch (HttpServerErrorException e) {
            throw new CmsApiException("BCDA server error downloading file: " + e.getStatusCode(), e);

        } catch (RestClientException e) {
            throw new CmsApiException("Network error downloading BCDA file: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel an in-progress export job
     * DELETE {jobUrl}
     *
     * @param jobUrl The Content-Location URL of the export job
     * @throws CmsApiException if cancellation fails
     */
    public void cancelExport(String jobUrl) {
        log.info("Cancelling BCDA export: {}", jobUrl);

        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            restTemplate.exchange(jobUrl, HttpMethod.DELETE, request, Void.class);
            log.info("Export cancelled successfully");

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                log.warn("Export job not found (may have already completed or expired)");
                return;
            }
            throw new CmsApiException("BCDA cancel error: " + e.getStatusCode(), e);

        } catch (RestClientException e) {
            throw new CmsApiException("Network error cancelling BCDA export: " + e.getMessage(), e);
        }
    }

    /**
     * Get BCDA API metadata (CapabilityStatement)
     * GET /api/v2/metadata
     *
     * @return BCDA metadata information
     */
    public BcdaMetadata getMetadata() {
        log.info("Fetching BCDA API metadata");

        String url = baseUrl + API_VERSION + "/metadata";

        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("BCDA API is available");
                return new BcdaMetadata("v2", baseUrl, true);
            }

            return new BcdaMetadata("unknown", baseUrl, false);

        } catch (RestClientException e) {
            log.error("BCDA metadata check failed: {}", e.getMessage());
            return new BcdaMetadata("unknown", baseUrl, false);
        }
    }

    /**
     * Check health of BCDA API
     *
     * @return Health status
     */
    public BcdaHealthStatus getHealthStatus() {
        log.info("Checking BCDA API health");

        try {
            BcdaMetadata metadata = getMetadata();
            if (metadata.isAvailable()) {
                return new BcdaHealthStatus("healthy", metadata.getApiVersion(), baseUrl);
            }
            return new BcdaHealthStatus("degraded", "unknown", baseUrl);

        } catch (Exception e) {
            log.error("BCDA health check failed: {}", e.getMessage());
            return new BcdaHealthStatus("unhealthy", "unknown", baseUrl);
        }
    }

    // ============ Private Methods ============

    private BulkDataExportResponse requestBulkExport(String path, String since) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl)
            .append(API_VERSION)
            .append(path);

        if (since != null && !since.isEmpty()) {
            urlBuilder.append("?_since=").append(since);
        }

        return executeExportRequest(urlBuilder.toString());
    }

    private BulkDataExportResponse executeExportRequest(String url) {
        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            ResponseEntity<Void> response = restTemplate.exchange(
                url, HttpMethod.GET, request, Void.class
            );

            // BCDA returns 202 Accepted with Content-Location header
            if (response.getStatusCode().value() == 202) {
                String jobUrl = response.getHeaders().getFirst("Content-Location");
                if (jobUrl != null) {
                    log.info("Export initiated, job URL: {}", jobUrl);
                    return new BulkDataExportResponse("accepted", jobUrl);
                }
            }

            throw new CmsApiException("Unexpected response from BCDA export request: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            throw new CmsApiException("BCDA export request error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);

        } catch (HttpServerErrorException e) {
            throw new CmsApiException("BCDA server error: " + e.getStatusCode(), e);

        } catch (RestClientException e) {
            throw new CmsApiException("Network error requesting BCDA export: " + e.getMessage(), e);
        }
    }

    private int parseProgress(String progressHeader) {
        if (progressHeader == null || progressHeader.isEmpty()) {
            return 0;
        }
        try {
            // Expected format: "50%" or "50 percent"
            String cleaned = progressHeader.replaceAll("[^0-9]", "");
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private BulkDataExportStatus parseExportStatusResponse(String responseBody) {
        // In production, this would parse the FHIR OperationOutcome or export manifest
        // For now, return a completed status with the raw response
        return new BulkDataExportStatus("complete", 100, responseBody, null);
    }

    // ============ DTOs ============

    public static class BulkDataExportResponse {
        private final String status;
        private final String jobUrl;

        public BulkDataExportResponse(String status, String jobUrl) {
            this.status = status;
            this.jobUrl = jobUrl;
        }

        public String getStatus() { return status; }
        public String getJobUrl() { return jobUrl; }
    }

    public static class BulkDataExportStatus {
        private final String status;
        private final int percentComplete;
        private final String outputManifest;
        private final String error;

        public BulkDataExportStatus(String status, int percentComplete, String outputManifest, String error) {
            this.status = status;
            this.percentComplete = percentComplete;
            this.outputManifest = outputManifest;
            this.error = error;
        }

        public String getStatus() { return status; }
        public int getPercentComplete() { return percentComplete; }
        public String getOutputManifest() { return outputManifest; }
        public String getError() { return error; }

        public boolean isComplete() { return "complete".equalsIgnoreCase(status); }
        public boolean isInProgress() { return "in-progress".equalsIgnoreCase(status); }
        public boolean hasError() { return error != null && !error.isEmpty(); }
    }

    public static class BcdaMetadata {
        private final String apiVersion;
        private final String endpoint;
        private final boolean available;

        public BcdaMetadata(String apiVersion, String endpoint, boolean available) {
            this.apiVersion = apiVersion;
            this.endpoint = endpoint;
            this.available = available;
        }

        public String getApiVersion() { return apiVersion; }
        public String getEndpoint() { return endpoint; }
        public boolean isAvailable() { return available; }
    }

    public static class BcdaHealthStatus {
        private final String status;
        private final String version;
        private final String endpoint;

        public BcdaHealthStatus(String status, String version, String endpoint) {
            this.status = status;
            this.version = version;
            this.endpoint = endpoint;
        }

        public String getStatus() { return status; }
        public String getVersion() { return version; }
        public String getEndpoint() { return endpoint; }

        public boolean isHealthy() { return "healthy".equalsIgnoreCase(status); }
    }
}
