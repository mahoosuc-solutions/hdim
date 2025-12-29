package com.healthdata.ecr.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.ecr.client.AimsApiClient;
import com.healthdata.ecr.persistence.ElectronicCaseReportEntity;
import com.healthdata.ecr.persistence.ElectronicCaseReportEntity.EcrStatus;
import com.healthdata.ecr.persistence.ElectronicCaseReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * eCR Processing Service
 *
 * Orchestrates the eCR workflow:
 * 1. Fetches patient/encounter data
 * 2. Generates eICR FHIR Bundle
 * 3. Transmits to AIMS platform
 * 4. Processes Reportability Responses
 *
 * Handles scheduling, retries, and error management.
 */
@Service
@Slf4j
public class EcrProcessingService {

    private final ElectronicCaseReportRepository ecrRepository;
    private final EicrGeneratorService eicrGenerator;
    private final AimsApiClient aimsClient;
    private final EcrDataFetchService dataFetchService;
    private final ObjectMapper objectMapper;
    private final IParser fhirJsonParser;

    private static final int MAX_RETRIES = 3;

    public EcrProcessingService(
            ElectronicCaseReportRepository ecrRepository,
            EicrGeneratorService eicrGenerator,
            AimsApiClient aimsClient,
            EcrDataFetchService dataFetchService,
            FhirContext fhirContext,
            ObjectMapper objectMapper) {
        this.ecrRepository = ecrRepository;
        this.eicrGenerator = eicrGenerator;
        this.aimsClient = aimsClient;
        this.dataFetchService = dataFetchService;
        this.objectMapper = objectMapper;
        this.fhirJsonParser = fhirContext.newJsonParser();
        this.fhirJsonParser.setPrettyPrint(false);
    }

    /**
     * Process immediate-urgency eCRs asynchronously.
     */
    @Async
    public void processImmediately(UUID ecrId) {
        log.info("Processing immediate eCR {}", ecrId);
        processEcr(ecrId);
    }

    /**
     * Scheduled job to process pending eCRs.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void processScheduledEcrs() {
        log.debug("Running scheduled eCR processing");

        // Process all pending eCRs (non-immediate ones) ordered by urgency
        List<ElectronicCaseReportEntity> pending = ecrRepository.findPendingByUrgency(null);

        // Filter by tenant - in production, iterate per tenant
        for (ElectronicCaseReportEntity ecr : pending) {
            if (shouldProcess(ecr)) {
                processEcr(ecr.getId());
            }
        }
    }

    /**
     * Scheduled job to retry failed eCRs.
     * Runs every 15 minutes.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    @Transactional
    public void retryFailedEcrs() {
        log.debug("Running eCR retry job");

        List<ElectronicCaseReportEntity> readyForRetry =
            ecrRepository.findReadyForRetry(MAX_RETRIES, LocalDateTime.now());

        for (ElectronicCaseReportEntity ecr : readyForRetry) {
            log.info("Retrying eCR {} (attempt {})", ecr.getId(), ecr.getRetryCount() + 1);
            processEcr(ecr.getId());
        }
    }

    /**
     * Scheduled job to check for Reportability Responses.
     * Runs every 30 minutes.
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    @Transactional
    public void checkReportabilityResponses() {
        log.debug("Checking for Reportability Responses");

        // Find eCRs submitted more than 5 minutes ago without RR
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        List<ElectronicCaseReportEntity> awaiting = ecrRepository.findAwaitingResponse(cutoff);

        for (ElectronicCaseReportEntity ecr : awaiting) {
            checkAndProcessRR(ecr);
        }
    }

    /**
     * Process a single eCR through the full workflow.
     */
    @Transactional
    public void processEcr(UUID ecrId) {
        ElectronicCaseReportEntity ecr = ecrRepository.findById(ecrId)
            .orElseThrow(() -> new IllegalArgumentException("eCR not found: " + ecrId));

        try {
            // Step 1: Update status to GENERATING
            ecr.setStatus(EcrStatus.GENERATING);
            ecr = ecrRepository.save(ecr);

            // Step 2: Fetch patient and encounter data
            EicrGeneratorService.PatientData patientData =
                dataFetchService.fetchPatientData(ecr.getTenantId(), ecr.getPatientId());

            EicrGeneratorService.EncounterData encounterData = null;
            if (ecr.getEncounterId() != null) {
                encounterData = dataFetchService.fetchEncounterData(
                    ecr.getTenantId(), ecr.getEncounterId());
            }

            EicrGeneratorService.TriggerData triggerData =
                EicrGeneratorService.TriggerData.builder()
                    .authorName("System Generated")
                    .onsetDate(ecr.getTriggerDetectedAt())
                    .build();

            // Step 3: Generate eICR Bundle
            Bundle eicrBundle = eicrGenerator.generateEicr(ecr, patientData, encounterData, triggerData);

            // Store bundle as JSON
            Map<String, Object> bundleJson = bundleToMap(eicrBundle);
            ecr.setEicrBundleJson(bundleJson);
            ecr.setEicrGeneratedAt(LocalDateTime.now());

            // Step 4: Update status to TRANSMITTING
            ecr.setStatus(EcrStatus.TRANSMITTING);
            ecr = ecrRepository.save(ecr);

            // Step 5: Submit to AIMS
            AimsApiClient.SubmissionResult result = aimsClient.submitEicr(eicrBundle);

            // Step 6: Update with submission result
            ecr.setAimsTrackingId(result.getTrackingId());
            ecr.setSubmittedAt(LocalDateTime.now());
            ecr.setStatus(EcrStatus.SUBMITTED);
            ecrRepository.save(ecr);

            log.info("Successfully submitted eCR {} to AIMS with tracking ID {}",
                ecrId, result.getTrackingId());

        } catch (Exception e) {
            handleProcessingError(ecr, e);
        }
    }

    /**
     * Check AIMS for Reportability Response and process it.
     */
    private void checkAndProcessRR(ElectronicCaseReportEntity ecr) {
        try {
            AimsApiClient.ReportabilityResponse rr =
                aimsClient.getReportabilityResponse(ecr.getAimsTrackingId());

            if (rr == null || rr.getStatus() == null) {
                log.debug("No RR available yet for eCR {}", ecr.getId());
                return;
            }

            // Update eCR with RR data
            ecr.setRrStatus(mapRRStatus(rr.getStatus()));
            ecr.setRrResponseJson(rr.getResponseJson());
            ecr.setJurisdiction(rr.getJurisdiction());
            ecr.setRrReceivedAt(LocalDateTime.now());
            ecr.setStatus(EcrStatus.ACKNOWLEDGED);

            ecrRepository.save(ecr);

            log.info("Processed RR for eCR {}: status={}, jurisdiction={}",
                ecr.getId(), rr.getStatus(), rr.getJurisdiction());

        } catch (Exception e) {
            log.warn("Error checking RR for eCR {}: {}", ecr.getId(), e.getMessage());
        }
    }

    private void handleProcessingError(ElectronicCaseReportEntity ecr, Exception e) {
        log.error("Error processing eCR {}: {}", ecr.getId(), e.getMessage(), e);

        ecr.setRetryCount(ecr.getRetryCount() + 1);
        ecr.setErrorMessage(e.getMessage());

        if (ecr.getRetryCount() >= MAX_RETRIES) {
            ecr.setStatus(EcrStatus.FAILED);
            log.error("eCR {} failed after {} retries", ecr.getId(), MAX_RETRIES);
        } else {
            ecr.setStatus(EcrStatus.FAILED);
            // Calculate exponential backoff
            long delayMinutes = (long) Math.pow(2, ecr.getRetryCount()) * 5;
            ecr.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
            log.info("eCR {} scheduled for retry at {}", ecr.getId(), ecr.getNextRetryAt());
        }

        ecrRepository.save(ecr);
    }

    private boolean shouldProcess(ElectronicCaseReportEntity ecr) {
        // Check urgency-based timing
        LocalDateTime detected = ecr.getTriggerDetectedAt();
        LocalDateTime now = LocalDateTime.now();

        return switch (ecr.getUrgency()) {
            case IMMEDIATE -> true; // Process immediately
            case WITHIN_24_HOURS -> detected.plusHours(23).isBefore(now) ||
                detected.plusMinutes(30).isBefore(now);
            case WITHIN_72_HOURS -> detected.plusHours(48).isBefore(now) ||
                detected.plusHours(1).isBefore(now);
            case ROUTINE -> detected.plusHours(4).isBefore(now);
        };
    }

    private ElectronicCaseReportEntity.ReportabilityStatus mapRRStatus(String status) {
        return switch (status.toUpperCase()) {
            case "REPORTABLE" -> ElectronicCaseReportEntity.ReportabilityStatus.REPORTABLE;
            case "MAY_BE_REPORTABLE" -> ElectronicCaseReportEntity.ReportabilityStatus.MAY_BE_REPORTABLE;
            case "NOT_REPORTABLE" -> ElectronicCaseReportEntity.ReportabilityStatus.NOT_REPORTABLE;
            default -> ElectronicCaseReportEntity.ReportabilityStatus.NO_RULE_MET;
        };
    }

    /**
     * Convert FHIR Bundle to Map for JSON storage in database.
     * Uses HAPI FHIR parser for accurate serialization.
     *
     * @param bundle The FHIR Bundle to convert
     * @return Map representation of the bundle for JSONB storage
     */
    private Map<String, Object> bundleToMap(Bundle bundle) {
        try {
            // Serialize Bundle to JSON using HAPI FHIR
            String bundleJson = fhirJsonParser.encodeResourceToString(bundle);

            // Parse JSON string to Map for JSONB storage
            return objectMapper.readValue(bundleJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to convert Bundle to Map: {}", e.getMessage());
            // Return minimal metadata on failure to avoid losing all information
            return Map.of(
                "resourceType", "Bundle",
                "id", bundle.getId() != null ? bundle.getId() : "unknown",
                "type", bundle.getType() != null ? bundle.getType().toCode() : "unknown",
                "entryCount", bundle.getEntry() != null ? bundle.getEntry().size() : 0,
                "_serializationError", e.getMessage()
            );
        }
    }
}
