package com.healthdata.caregap.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.caregap.client.CqlEngineServiceClient;
import com.healthdata.caregap.client.PatientServiceClient;
import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Care Gap Identification Service
 *
 * Identifies care gaps using CQL (Clinical Quality Language) rules evaluation.
 * Integrates with Care Gap Service for patient data and CQL Engine for rule processing.
 *
 * Supports HEDIS, CMS, and custom quality measures for:
 * - Preventive care gaps (immunizations, screenings)
 * - Chronic disease management (diabetes, hypertension)
 * - Medication adherence
 * - Follow-up care
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareGapIdentificationService {

    private final CareGapRepository careGapRepository;
    private final PatientServiceClient patientServiceClient;
    private final CqlEngineServiceClient cqlEngineServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CareGapAuditIntegration careGapAuditIntegration;
    private final FhirContext fhirContext = FhirContext.forR4();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Identify all care gaps for a patient
     *
     * Evaluates patient against all available CQL libraries and identifies gaps.
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param createdBy User performing gap identification
     * @return List of identified care gaps
     */
    @Transactional
    public List<CareGapEntity> identifyAllCareGaps(String tenantId, UUID patientId, String createdBy) {
        log.info("Identifying all care gaps for patient: {} in tenant: {}", patientId, tenantId);

        List<CareGapEntity> gaps = new ArrayList<>();

        // Get available CQL libraries
        String librariesJson = cqlEngineServiceClient.getAvailableLibraries();
        List<String> libraries = parseLibraryNames(librariesJson);

        // Evaluate each library
        for (String library : libraries) {
            try {
                List<CareGapEntity> libraryGaps = identifyCareGapsForLibrary(
                        tenantId, patientId, library, createdBy);
                gaps.addAll(libraryGaps);
            } catch (Exception e) {
                log.error("Error evaluating library {} for patient {}: {}",
                        library, patientId, e.getMessage());
            }
        }

        // Publish gap identification event
        publishGapIdentificationEvent(tenantId, patientId, gaps.size());

        log.info("Identified {} care gaps for patient: {}", gaps.size(), patientId);
        return gaps;
    }

    /**
     * Identify care gaps for a specific CQL library/measure
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param libraryName CQL library name (e.g., "HEDIS_CDC_A1C")
     * @param createdBy User performing gap identification
     * @return List of identified care gaps
     */
    @Transactional
    public List<CareGapEntity> identifyCareGapsForLibrary(
            String tenantId,
            UUID patientId,
            String libraryName,
            String createdBy
    ) {
        log.info("Evaluating CQL library {} for patient: {}", libraryName, patientId);

        List<CareGapEntity> gaps = new ArrayList<>();

        // Evaluate CQL library
        String cqlResultJson = cqlEngineServiceClient.evaluateCql(
                tenantId, libraryName, patientId, null);

        // Parse CQL result
        JsonNode cqlResult = parseCqlResult(cqlResultJson);

        // Check if gap exists
        boolean hasGap = evaluateGapFromCqlResult(cqlResult);

        if (hasGap) {
            // Create care gap entity
            CareGapEntity gap = createCareGapFromCqlResult(
                    tenantId, patientId, libraryName, cqlResult, createdBy);

            // Save gap
            CareGapEntity saved = careGapRepository.save(gap);
            gaps.add(saved);

            log.info("Created care gap: {} for measure: {}", saved.getId(), libraryName);

            // Publish audit event for AI-driven care gap identification
            careGapAuditIntegration.publishCareGapIdentificationEvent(
                    tenantId,
                    patientId.toString(),
                    libraryName,
                    saved.getId().toString(),
                    cqlResult,
                    createdBy
            );
        }

        return gaps;
    }

    /**
     * Refresh care gaps for a patient (re-evaluate all measures)
     *
     * Closes resolved gaps and identifies new gaps.
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param createdBy User performing refresh
     * @return List of active care gaps
     */
    @Transactional
    public List<CareGapEntity> refreshCareGaps(String tenantId, UUID patientId, String createdBy) {
        log.info("Refreshing care gaps for patient: {}", patientId);

        // Get existing open gaps
        List<CareGapEntity> existingGaps = careGapRepository.findOpenGapsByPatient(
                tenantId, patientId);

        // Re-evaluate all measures
        List<CareGapEntity> currentGaps = identifyAllCareGaps(tenantId, patientId, createdBy);

        // Close gaps that are no longer present
        Set<String> currentMeasureIds = new HashSet<>();
        currentGaps.forEach(gap -> currentMeasureIds.add(gap.getMeasureId()));

        for (CareGapEntity existingGap : existingGaps) {
            if (!currentMeasureIds.contains(existingGap.getMeasureId())) {
                existingGap.setGapStatus("CLOSED");
                existingGap.setClosedDate(Instant.now());
                existingGap.setClosedBy("system");
                existingGap.setClosureReason("Gap resolved - measure criteria now met");
                careGapRepository.save(existingGap);

                log.info("Auto-closed gap: {} for measure: {}",
                        existingGap.getId(), existingGap.getMeasureId());
            }
        }

        return currentGaps;
    }

    /**
     * Close a care gap manually
     *
     * @param tenantId Tenant ID
     * @param gapId Gap ID
     * @param closedBy User closing the gap
     * @param closureReason Reason for closure
     * @param closureAction Action taken to close gap
     * @return Updated care gap
     */
    @Transactional
    public CareGapEntity closeCareGap(
            String tenantId,
            UUID gapId,
            String closedBy,
            String closureReason,
            String closureAction
    ) {
        log.info("Closing care gap: {} by: {}", gapId, closedBy);

        CareGapEntity gap = careGapRepository.findByIdAndTenantId(gapId, tenantId)
                .orElseThrow(() -> new RuntimeException("Care gap not found: " + gapId));

        gap.setGapStatus("CLOSED");
        gap.setClosedDate(Instant.now());
        gap.setClosedBy(closedBy);
        gap.setClosureReason(closureReason);
        gap.setClosureAction(closureAction);
        gap.setUpdatedBy(closedBy);

        CareGapEntity saved = careGapRepository.save(gap);

        // Publish gap closure event
        publishGapClosureEvent(tenantId, gapId.toString(), closedBy);

        // Publish audit event for care gap closure
        careGapAuditIntegration.publishCareGapClosureEvent(
                tenantId,
                gap.getPatientId().toString(),
                gap.getMeasureId(),
                gapId.toString(),
                closedBy,
                closureReason,
                closureAction
        );

        return saved;
    }

    /**
     * Get open care gaps for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return List of open care gaps
     */
    @Cacheable(value = "patientCareGaps", key = "#tenantId + ':' + #patientId")
    public List<CareGapEntity> getOpenCareGaps(String tenantId, UUID patientId) {
        return careGapRepository.findOpenGapsByPatient(tenantId, patientId);
    }

    /**
     * Get high priority care gaps for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return List of high priority care gaps
     */
    public List<CareGapEntity> getHighPriorityCareGaps(String tenantId, UUID patientId) {
        return careGapRepository.findHighPriorityOpenGaps(tenantId, patientId);
    }

    // ==================== Bulk Operations (Issue #241) ====================

    /**
     * Bulk close multiple care gaps
     *
     * Issue #241: Care Gap Bulk Actions
     * Processes multiple gap closures in a single operation, tracking success/failure for each gap.
     *
     * @param tenantId Tenant ID
     * @param request Bulk closure request with gap IDs, reason, and notes
     * @return Bulk operation response with success/failure counts and error details
     */
    @Transactional
    public com.healthdata.caregap.dto.BulkOperationResponse bulkCloseCareGaps(
            String tenantId,
            com.healthdata.caregap.dto.BulkClosureRequest request
    ) {
        long startTime = System.currentTimeMillis();
        log.info("Bulk closing {} care gaps for tenant: {}", request.getGapIds().size(), tenantId);

        List<String> successfulGapIds = new ArrayList<>();
        List<com.healthdata.caregap.dto.BulkOperationError> errors = new ArrayList<>();

        for (String gapIdStr : request.getGapIds()) {
            try {
                UUID gapId = UUID.fromString(gapIdStr);

                // Close the gap using existing method
                closeCareGap(
                    tenantId,
                    gapId,
                    request.getClosedBy(),
                    request.getClosureReason(),
                    request.getClosureAction() != null ? request.getClosureAction() : request.getNotes()
                );

                successfulGapIds.add(gapIdStr);
                log.debug("Successfully closed gap: {}", gapIdStr);

            } catch (IllegalArgumentException e) {
                log.error("Invalid gap ID format: {}", gapIdStr, e);
                errors.add(com.healthdata.caregap.dto.BulkOperationError.builder()
                        .gapId(gapIdStr)
                        .errorMessage("Invalid gap ID format")
                        .errorCode("INVALID_GAP_ID")
                        .details(e.getMessage())
                        .build());

            } catch (RuntimeException e) {
                log.error("Failed to close gap: {}", gapIdStr, e);
                errors.add(com.healthdata.caregap.dto.BulkOperationError.builder()
                        .gapId(gapIdStr)
                        .errorMessage(e.getMessage())
                        .errorCode("CLOSURE_FAILED")
                        .details(e.getClass().getSimpleName())
                        .build());

            } catch (Exception e) {
                log.error("Unexpected error closing gap: {}", gapIdStr, e);
                errors.add(com.healthdata.caregap.dto.BulkOperationError.builder()
                        .gapId(gapIdStr)
                        .errorMessage("Unexpected error occurred")
                        .errorCode("INTERNAL_ERROR")
                        .details(e.getMessage())
                        .build());
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        log.info("Bulk close completed: {} successful, {} failed, {}ms",
                successfulGapIds.size(), errors.size(), processingTime);

        // Publish bulk closure event
        publishBulkClosureEvent(tenantId, successfulGapIds.size(), errors.size());

        return com.healthdata.caregap.dto.BulkOperationResponse.builder()
                .totalRequested(request.getGapIds().size())
                .successCount(successfulGapIds.size())
                .failureCount(errors.size())
                .successfulGapIds(successfulGapIds)
                .errors(errors)
                .processingTimeMs(processingTime)
                .message(String.format("Closed %d of %d care gaps",
                        successfulGapIds.size(), request.getGapIds().size()))
                .build();
    }

    /**
     * Bulk assign intervention to multiple care gaps
     *
     * Issue #241: Care Gap Bulk Actions
     *
     * @param tenantId Tenant ID
     * @param request Bulk intervention request
     * @return Bulk operation response
     */
    @Transactional
    public com.healthdata.caregap.dto.BulkOperationResponse bulkAssignIntervention(
            String tenantId,
            com.healthdata.caregap.dto.BulkInterventionRequest request
    ) {
        long startTime = System.currentTimeMillis();
        log.info("Bulk assigning intervention to {} care gaps", request.getGapIds().size());

        List<String> successfulGapIds = new ArrayList<>();
        List<com.healthdata.caregap.dto.BulkOperationError> errors = new ArrayList<>();

        for (String gapIdStr : request.getGapIds()) {
            try {
                UUID gapId = UUID.fromString(gapIdStr);

                CareGapEntity gap = careGapRepository.findByIdAndTenantId(gapId, tenantId)
                        .orElseThrow(() -> new RuntimeException("Care gap not found: " + gapId));

                // Update gap with intervention information
                gap.setRecommendation(request.getDescription());
                gap.setRecommendationType(request.getInterventionType());
                gap.setRecommendedAction(request.getDescription());
                gap.setUpdatedBy(request.getAssignedTo() != null ?
                        request.getAssignedTo() : "system");

                careGapRepository.save(gap);
                successfulGapIds.add(gapIdStr);

                log.debug("Successfully assigned intervention to gap: {}", gapIdStr);

            } catch (Exception e) {
                log.error("Failed to assign intervention to gap: {}", gapIdStr, e);
                errors.add(com.healthdata.caregap.dto.BulkOperationError.builder()
                        .gapId(gapIdStr)
                        .errorMessage(e.getMessage())
                        .errorCode("INTERVENTION_ASSIGNMENT_FAILED")
                        .build());
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        log.info("Bulk intervention assignment completed: {} successful, {} failed",
                successfulGapIds.size(), errors.size());

        return com.healthdata.caregap.dto.BulkOperationResponse.builder()
                .totalRequested(request.getGapIds().size())
                .successCount(successfulGapIds.size())
                .failureCount(errors.size())
                .successfulGapIds(successfulGapIds)
                .errors(errors)
                .processingTimeMs(processingTime)
                .message(String.format("Assigned intervention to %d of %d care gaps",
                        successfulGapIds.size(), request.getGapIds().size()))
                .build();
    }

    /**
     * Bulk update priority for multiple care gaps
     *
     * Issue #241: Care Gap Bulk Actions
     *
     * @param tenantId Tenant ID
     * @param request Bulk priority update request
     * @return Bulk operation response
     */
    @Transactional
    public com.healthdata.caregap.dto.BulkOperationResponse bulkUpdatePriority(
            String tenantId,
            com.healthdata.caregap.dto.BulkPriorityUpdateRequest request
    ) {
        long startTime = System.currentTimeMillis();
        log.info("Bulk updating priority to {} for {} care gaps",
                request.getPriority(), request.getGapIds().size());

        List<String> successfulGapIds = new ArrayList<>();
        List<com.healthdata.caregap.dto.BulkOperationError> errors = new ArrayList<>();

        for (String gapIdStr : request.getGapIds()) {
            try {
                UUID gapId = UUID.fromString(gapIdStr);

                CareGapEntity gap = careGapRepository.findByIdAndTenantId(gapId, tenantId)
                        .orElseThrow(() -> new RuntimeException("Care gap not found: " + gapId));

                // Update gap priority
                gap.setPriority(request.getPriority());
                gap.setUpdatedBy("system"); // Could be parameterized if needed

                careGapRepository.save(gap);
                successfulGapIds.add(gapIdStr);

                log.debug("Successfully updated priority for gap: {}", gapIdStr);

            } catch (Exception e) {
                log.error("Failed to update priority for gap: {}", gapIdStr, e);
                errors.add(com.healthdata.caregap.dto.BulkOperationError.builder()
                        .gapId(gapIdStr)
                        .errorMessage(e.getMessage())
                        .errorCode("PRIORITY_UPDATE_FAILED")
                        .build());
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        log.info("Bulk priority update completed: {} successful, {} failed",
                successfulGapIds.size(), errors.size());

        return com.healthdata.caregap.dto.BulkOperationResponse.builder()
                .totalRequested(request.getGapIds().size())
                .successCount(successfulGapIds.size())
                .failureCount(errors.size())
                .successfulGapIds(successfulGapIds)
                .errors(errors)
                .processingTimeMs(processingTime)
                .message(String.format("Updated priority for %d of %d care gaps",
                        successfulGapIds.size(), request.getGapIds().size()))
                .build();
    }

    // ==================== Statistics Methods ====================

    /**
     * Get care gap statistics for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Care gap statistics
     */
    public CareGapStats getCareGapStats(String tenantId, UUID patientId) {
        long openGapsCount = careGapRepository.countOpenGaps(tenantId, patientId);
        long highPriorityCount = careGapRepository.countHighPriorityGaps(tenantId, patientId);
        long overdueCount = careGapRepository.countOverdueGaps(tenantId, patientId, LocalDate.now());

        return new CareGapStats(
                openGapsCount,
                highPriorityCount,
                overdueCount,
                openGapsCount > 0,
                highPriorityCount > 0
        );
    }

    // ==================== Private Helper Methods ====================

    private List<String> parseLibraryNames(String librariesJson) {
        try {
            JsonNode root = objectMapper.readTree(librariesJson);
            List<String> libraries = new ArrayList<>();

            if (root.isArray()) {
                root.forEach(node -> {
                    if (node.has("name")) {
                        libraries.add(node.get("name").asText());
                    }
                });
            }

            return libraries;
        } catch (Exception e) {
            log.error("Error parsing libraries JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private JsonNode parseCqlResult(String cqlResultJson) {
        try {
            return objectMapper.readTree(cqlResultJson);
        } catch (Exception e) {
            log.error("Error parsing CQL result: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private boolean evaluateGapFromCqlResult(JsonNode cqlResult) {
        // Check if CQL result indicates a gap
        // Convention: gap exists if "hasGap" = true or "inNumerator" = false
        if (cqlResult.has("hasGap")) {
            return cqlResult.get("hasGap").asBoolean();
        }
        if (cqlResult.has("inNumerator")) {
            return !cqlResult.get("inNumerator").asBoolean();
        }
        return false;
    }

    private CareGapEntity createCareGapFromCqlResult(
            String tenantId,
            UUID patientId,
            String libraryName,
            JsonNode cqlResult,
            String createdBy
    ) {
        return CareGapEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(extractMeasureId(libraryName, cqlResult))
                .measureName(extractMeasureName(libraryName, cqlResult))
                .gapCategory(extractMeasureCategory(libraryName))
                .measureYear(LocalDate.now().getYear())
                .gapType("care-gap")
                .gapStatus("OPEN")
                .gapDescription(extractGapDescription(cqlResult))
                .gapReason(extractGapReason(cqlResult))
                .priority(extractPriority(cqlResult))
                .severity(extractSeverity(cqlResult))
                .riskScore(extractRiskScore(cqlResult))
                .identifiedDate(Instant.now())
                .dueDate(extractDueDate(cqlResult))
                .recommendation(extractRecommendation(cqlResult))
                .recommendationType(extractRecommendationType(cqlResult))
                .recommendedAction(extractRecommendedAction(cqlResult))
                .cqlLibrary(libraryName)
                .cqlExpression("Main")
                .cqlResult(cqlResult.toString())
                .createdBy(createdBy)
                .build();
    }

    private String extractMeasureId(String libraryName, JsonNode cqlResult) {
        if (cqlResult.has("measureId")) {
            return cqlResult.get("measureId").asText();
        }
        return libraryName;
    }

    private String extractMeasureName(String libraryName, JsonNode cqlResult) {
        if (cqlResult.has("measureName")) {
            return cqlResult.get("measureName").asText();
        }
        return libraryName.replace("_", " ");
    }

    private String extractMeasureCategory(String libraryName) {
        if (libraryName.startsWith("HEDIS_")) {
            return "HEDIS";
        } else if (libraryName.startsWith("CMS_")) {
            return "CMS";
        }
        return "custom";
    }

    private String extractGapDescription(JsonNode cqlResult) {
        if (cqlResult.has("gapDescription")) {
            return cqlResult.get("gapDescription").asText();
        }
        return "Care gap identified";
    }

    private String extractGapReason(JsonNode cqlResult) {
        if (cqlResult.has("gapReason")) {
            return cqlResult.get("gapReason").asText();
        }
        return "Measure criteria not met";
    }

    private String extractPriority(JsonNode cqlResult) {
        if (cqlResult.has("priority")) {
            return cqlResult.get("priority").asText();
        }
        return "medium";
    }

    private String extractSeverity(JsonNode cqlResult) {
        if (cqlResult.has("severity")) {
            return cqlResult.get("severity").asText();
        }
        if (cqlResult.has("priority")) {
            return cqlResult.get("priority").asText();
        }
        return "medium";
    }

    private Double extractRiskScore(JsonNode cqlResult) {
        if (cqlResult.has("riskScore")) {
            return cqlResult.get("riskScore").asDouble();
        }
        return 0.5;
    }

    private LocalDate extractDueDate(JsonNode cqlResult) {
        if (cqlResult.has("dueDate")) {
            try {
                return LocalDate.parse(cqlResult.get("dueDate").asText());
            } catch (Exception e) {
                log.warn("Could not parse due date: {}", e.getMessage());
            }
        }
        return LocalDate.now().plusDays(90); // Default: 90 days
    }

    private String extractRecommendation(JsonNode cqlResult) {
        if (cqlResult.has("recommendation")) {
            return cqlResult.get("recommendation").asText();
        }
        return null;
    }

    private String extractRecommendationType(JsonNode cqlResult) {
        if (cqlResult.has("recommendationType")) {
            return cqlResult.get("recommendationType").asText();
        }
        return null;
    }

    private String extractRecommendedAction(JsonNode cqlResult) {
        if (cqlResult.has("recommendedAction")) {
            return cqlResult.get("recommendedAction").asText();
        }
        return null;
    }

    private void publishGapIdentificationEvent(String tenantId, UUID patientId, int gapCount) {
        try {
            String event = String.format("{\"tenantId\":\"%s\",\"patientId\":\"%s\",\"gapCount\":%d,\"timestamp\":\"%s\"}",
                    tenantId, patientId, gapCount, LocalDate.now());
            kafkaTemplate.send("care-gap-identified", event);
        } catch (Exception e) {
            log.error("Error publishing gap identification event: {}", e.getMessage());
        }
    }

    private void publishGapClosureEvent(String tenantId, String gapId, String closedBy) {
        try {
            String event = String.format("{\"tenantId\":\"%s\",\"gapId\":\"%s\",\"closedBy\":\"%s\",\"timestamp\":\"%s\"}",
                    tenantId, gapId, closedBy, LocalDate.now());
            kafkaTemplate.send("care-gap-closed", event);
        } catch (Exception e) {
            log.error("Error publishing gap closure event: {}", e.getMessage());
        }
    }

    private void publishBulkClosureEvent(String tenantId, int successCount, int failureCount) {
        try {
            String event = String.format("{\"tenantId\":\"%s\",\"successCount\":%d,\"failureCount\":%d,\"timestamp\":\"%s\"}",
                    tenantId, successCount, failureCount, LocalDate.now());
            kafkaTemplate.send("care-gap-bulk-closed", event);
        } catch (Exception e) {
            log.error("Error publishing bulk closure event: {}", e.getMessage());
        }
    }

    /**
     * Care gap statistics record
     */
    public record CareGapStats(
            long openGapsCount,
            long highPriorityCount,
            long overdueCount,
            boolean hasOpenGaps,
            boolean hasHighPriorityGaps
    ) {}
}
