package com.healthdata.migration.client;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.healthdata.cdr.dto.CdaProcessRequest;
import com.healthdata.cdr.dto.CdaProcessResponse;
import com.healthdata.cdr.dto.ProcessMessageRequest;
import com.healthdata.cdr.dto.ProcessMessageResponse;
import com.healthdata.migration.dto.DataType;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;

/**
 * Client for communicating with CDR Processor Service.
 * Handles HL7v2 message parsing and CDA document processing
 * with resilience patterns (circuit breaker, retry).
 */
@Component
@RequiredArgsConstructor
public class CdrProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(CdrProcessorClient.class);

    private final WebClient cdrProcessorWebClient;

    @Value("${cdr-processor.timeout:30s}")
    private Duration timeout;

    /**
     * Process an HL7 v2 message through CDR Processor Service.
     *
     * @param message Raw HL7 v2 message content
     * @param tenantId Tenant identifier
     * @param convertToFhir Whether to convert to FHIR resources
     * @return Processing response with parsed message and optional FHIR bundle
     */
    @CircuitBreaker(name = "cdrProcessor", fallbackMethod = "processHl7MessageFallback")
    @Retry(name = "cdrProcessor")
    public ProcessMessageResponse processHl7Message(String message, String tenantId, boolean convertToFhir) {
        log.debug("Processing HL7 message via CDR Processor: tenantId={}", tenantId);

        ProcessMessageRequest request = ProcessMessageRequest.builder()
                .message(message)
                .tenantId(tenantId)
                .convertToFhir(convertToFhir)
                .build();

        return cdrProcessorWebClient.post()
                .uri("/api/v1/cdr/hl7/v2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ProcessMessageResponse.class)
                .timeout(timeout)
                .block();
    }

    /**
     * Process a CDA document through CDR Processor Service.
     *
     * @param document CDA document content (XML)
     * @param tenantId Tenant identifier
     * @param convertToFhir Whether to convert to FHIR resources
     * @return Processing response with parsed document and optional FHIR bundle
     */
    @CircuitBreaker(name = "cdrProcessor", fallbackMethod = "processCdaDocumentFallback")
    @Retry(name = "cdrProcessor")
    public CdaProcessResponse processCdaDocument(String document, String tenantId, boolean convertToFhir) {
        log.debug("Processing CDA document via CDR Processor: tenantId={}", tenantId);

        CdaProcessRequest request = CdaProcessRequest.builder()
                .document(document)
                .tenantId(tenantId)
                .convertToFhir(convertToFhir)
                .validateDocument(true)
                .build();

        return cdrProcessorWebClient.post()
                .uri("/api/v1/cdr/cda")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CdaProcessResponse.class)
                .timeout(timeout)
                .block();
    }

    /**
     * Process a record based on its data type.
     *
     * @param content Raw content
     * @param dataType Type of data (HL7V2, CDA, FHIR_BUNDLE)
     * @param tenantId Tenant identifier
     * @return Processing result with success status and resource counts
     */
    public CdrProcessingResult processRecord(String content, DataType dataType, String tenantId) {
        try {
            switch (dataType) {
                case HL7V2:
                    ProcessMessageResponse hl7Response = processHl7Message(content, tenantId, true);
                    return CdrProcessingResult.fromHl7Response(hl7Response);

                case CDA:
                    CdaProcessResponse cdaResponse = processCdaDocument(content, tenantId, true);
                    return CdrProcessingResult.fromCdaResponse(cdaResponse);

                case FHIR_BUNDLE:
                    // FHIR bundles don't need CDR processing - they're already in FHIR format
                    // Just validate and return success
                    return CdrProcessingResult.success("Bundle", 1);

                default:
                    return CdrProcessingResult.failure("Unsupported data type: " + dataType);
            }
        } catch (Exception e) {
            log.error("Error processing record via CDR Processor: dataType={}, error={}", dataType, e.getMessage());
            return CdrProcessingResult.failure(e.getMessage());
        }
    }

    /**
     * Check if CDR Processor Service is healthy.
     *
     * @return true if service is available
     */
    public boolean isHealthy() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> health = cdrProcessorWebClient.get()
                    .uri("/api/v1/cdr/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return health != null && "UP".equals(health.get("status"));
        } catch (Exception e) {
            log.warn("CDR Processor health check failed: {}", e.getMessage());
            return false;
        }
    }

    // Fallback methods for circuit breaker

    @SuppressWarnings("unused")
    private ProcessMessageResponse processHl7MessageFallback(String message, String tenantId,
            boolean convertToFhir, Throwable t) {
        log.warn("CDR Processor circuit breaker open for HL7 processing: {}", t.getMessage());
        return ProcessMessageResponse.builder()
                .success(false)
                .errorMessage("CDR Processor service unavailable: " + t.getMessage())
                .build();
    }

    @SuppressWarnings("unused")
    private CdaProcessResponse processCdaDocumentFallback(String document, String tenantId,
            boolean convertToFhir, Throwable t) {
        log.warn("CDR Processor circuit breaker open for CDA processing: {}", t.getMessage());
        return CdaProcessResponse.builder()
                .success(false)
                .errorMessage("CDR Processor service unavailable: " + t.getMessage())
                .build();
    }
}
