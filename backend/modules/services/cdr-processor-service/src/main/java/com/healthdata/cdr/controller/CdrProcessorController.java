package com.healthdata.cdr.controller;

import com.healthdata.cdr.converter.Hl7ToFhirConverter;
import com.healthdata.cdr.dto.*;
import com.healthdata.cdr.service.Hl7v2ParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST controller for CDR Processor Service.
 *
 * Provides endpoints for:
 * - HL7 v2 message parsing
 * - Batch message processing
 * - HL7 to FHIR conversion
 * - Service health checks
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cdr")
@RequiredArgsConstructor
@Tag(name = "CDR Processor", description = "Clinical Data Repository message processing")
public class CdrProcessorController {

    private final Hl7v2ParserService hl7v2ParserService;
    private final Hl7ToFhirConverter hl7ToFhirConverter;

    /**
     * Parse and process a single HL7 v2 message.
     *
     * @param request Process message request
     * @param authentication Current user authentication
     * @return Processed message response
     */
    @PostMapping("/hl7/v2")
    @Operation(summary = "Parse HL7 v2 message",
               description = "Parse and process a single HL7 v2 message (ADT, ORU, ORM)")
    public ResponseEntity<ProcessMessageResponse> processMessage(
        @Valid @RequestBody ProcessMessageRequest request,
        Authentication authentication
    ) {
        log.info("Processing HL7 v2 message: tenantId={}, user={}",
            request.getTenantId(),
            authentication != null ? authentication.getName() : "anonymous");

        long startTime = System.currentTimeMillis();

        try {
            // Validate message format
            if (!hl7v2ParserService.validateMessage(request.getMessage())) {
                return ResponseEntity.badRequest()
                    .body(ProcessMessageResponse.builder()
                        .success(false)
                        .errorMessage("Invalid HL7 message format")
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .build());
            }

            // Parse message
            Hl7v2Message parsedMessage = hl7v2ParserService.parseMessage(
                request.getMessage(),
                request.getTenantId()
            );

            // Check for parsing errors
            if ("ERROR".equals(parsedMessage.getStatus())) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ProcessMessageResponse.builder()
                        .parsedMessage(parsedMessage)
                        .success(false)
                        .errorMessage(parsedMessage.getErrorMessage())
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .build());
            }

            // Convert to FHIR if requested
            Bundle fhirBundle = null;
            if (request.isConvertToFhir()) {
                fhirBundle = hl7ToFhirConverter.convertToFhir(parsedMessage);
            }

            long processingTime = System.currentTimeMillis() - startTime;

            log.info("Successfully processed HL7 message: type={}, controlId={}, time={}ms",
                parsedMessage.getMessageCode(),
                parsedMessage.getMessageControlId(),
                processingTime);

            return ResponseEntity.ok(ProcessMessageResponse.builder()
                .parsedMessage(parsedMessage)
                .fhirBundle(fhirBundle)
                .success(true)
                .processingTimeMs(processingTime)
                .build());

        } catch (Exception e) {
            log.error("Error processing HL7 message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProcessMessageResponse.builder()
                    .success(false)
                    .errorMessage("Processing error: " + e.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build());
        }
    }

    /**
     * Batch process multiple HL7 v2 messages.
     *
     * @param request Batch process request
     * @param authentication Current user authentication
     * @return Batch process response
     */
    @PostMapping("/hl7/v2/batch")
    @Operation(summary = "Batch process HL7 v2 messages",
               description = "Process multiple HL7 v2 messages in a single request")
    public ResponseEntity<BatchProcessResponse> batchProcessMessages(
        @Valid @RequestBody BatchProcessRequest request,
        Authentication authentication
    ) {
        log.info("Batch processing {} HL7 v2 messages: tenantId={}, user={}",
            request.getMessages().size(),
            request.getTenantId(),
            authentication != null ? authentication.getName() : "anonymous");

        long startTime = System.currentTimeMillis();

        List<Hl7v2Message> processedMessages = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (String rawMessage : request.getMessages()) {
            try {
                // Validate and parse message
                if (!hl7v2ParserService.validateMessage(rawMessage)) {
                    failureCount++;
                    continue;
                }

                Hl7v2Message parsedMessage = hl7v2ParserService.parseMessage(
                    rawMessage,
                    request.getTenantId()
                );

                processedMessages.add(parsedMessage);

                if ("PARSED".equals(parsedMessage.getStatus())) {
                    successCount++;
                } else {
                    failureCount++;
                }

            } catch (Exception e) {
                log.error("Error processing message in batch: {}", e.getMessage());
                failureCount++;
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        log.info("Batch processing completed: total={}, success={}, failure={}, time={}ms",
            request.getMessages().size(), successCount, failureCount, processingTime);

        return ResponseEntity.ok(BatchProcessResponse.builder()
            .totalMessages(request.getMessages().size())
            .successCount(successCount)
            .failureCount(failureCount)
            .processedMessages(processedMessages)
            .processingTimeMs(processingTime)
            .build());
    }

    /**
     * Get CDR Processor service status.
     *
     * @return Service status information
     */
    @GetMapping("/status")
    @Operation(summary = "Get service status",
               description = "Get CDR Processor service status and capabilities")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "service", "cdr-processor-service",
            "status", "UP",
            "supportedMessageTypes", List.of("ADT", "ORU", "ORM"),
            "supportedVersions", List.of("2.5", "2.6"),
            "features", Map.of(
                "hl7v2Parsing", true,
                "fhirConversion", true,
                "batchProcessing", true,
                "multiTenant", true
            )
        ));
    }

    /**
     * Health check endpoint.
     *
     * @return Health status
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check service health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "cdr-processor-service"
        ));
    }

    /**
     * Get supported message types.
     *
     * @return List of supported message types with descriptions
     */
    @GetMapping("/message-types")
    @Operation(summary = "Get supported message types",
               description = "Get list of supported HL7 v2 message types")
    public ResponseEntity<List<Map<String, String>>> getSupportedMessageTypes() {
        List<Map<String, String>> messageTypes = List.of(
            Map.of(
                "type", "ADT^A01",
                "description", "Admit/Visit notification"
            ),
            Map.of(
                "type", "ADT^A02",
                "description", "Transfer a patient"
            ),
            Map.of(
                "type", "ADT^A03",
                "description", "Discharge/End visit"
            ),
            Map.of(
                "type", "ADT^A04",
                "description", "Register a patient"
            ),
            Map.of(
                "type", "ADT^A08",
                "description", "Update patient information"
            ),
            Map.of(
                "type", "ORU^R01",
                "description", "Unsolicited observation/lab results"
            ),
            Map.of(
                "type", "ORM^O01",
                "description", "General order message"
            )
        );

        return ResponseEntity.ok(messageTypes);
    }
}
