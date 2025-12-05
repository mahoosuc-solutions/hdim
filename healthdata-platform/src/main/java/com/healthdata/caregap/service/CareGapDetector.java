package com.healthdata.caregap.service;

import com.healthdata.patient.service.PatientService;
import com.healthdata.fhir.service.FhirService;
import com.healthdata.quality.domain.MeasureResult;
import com.healthdata.quality.service.QualityMeasureService;
import com.healthdata.caregap.domain.CareGap;
import com.healthdata.caregap.repository.CareGapRepository;
import com.healthdata.caregap.events.CareGapDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Care Gap Detection Service - Core module for identifying gaps in care
 * This is a KEY BENEFIT of the modular monolith: direct service injection!
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CareGapDetector {

    // Direct injection of other module services - no REST calls!
    private final PatientService patientService;
    private final FhirService fhirService;
    private final QualityMeasureService qualityMeasureService;
    private final CareGapRepository careGapRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Detects care gaps for a specific patient
     * This demonstrates the power of modular monolith - direct method calls!
     */
    public List<CareGap> detectCareGaps(String patientId) {
        log.info("Detecting care gaps for patient: {}", patientId);

        List<CareGap> detectedGaps = new ArrayList<>();

        // Direct call to patient service - no REST API needed!
        var patient = patientService.getPatient(patientId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        // Get patient's clinical data directly from FHIR service
        var observations = fhirService.getObservationsForPatient(patientId);
        var conditions = fhirService.getConditionsForPatient(patientId);
        var medications = fhirService.getMedicationsForPatient(patientId);

        // Check various care gap categories
        detectedGaps.addAll(detectPreventiveCareGaps(patient, observations));
        detectedGaps.addAll(detectChronicDiseaseGaps(patient, conditions, observations));
        detectedGaps.addAll(detectMedicationAdherenceGaps(patient, medications));
        detectedGaps.addAll(detectScreeningGaps(patient, observations));

        // Save detected gaps
        detectedGaps = careGapRepository.saveAll(detectedGaps);

        // Publish events for each detected gap
        detectedGaps.forEach(gap -> {
            eventPublisher.publishEvent(new CareGapDetectedEvent(
                gap.getId(),
                gap.getPatientId(),
                gap.getGapType(),
                gap.getPriority()
            ));
        });

        log.info("Detected {} care gaps for patient: {}", detectedGaps.size(), patientId);
        return detectedGaps;
    }

    /**
     * Batch detection for multiple patients - async processing
     */
    @Async
    public void detectCareGapsBatch(List<String> patientIds) {
        log.info("Starting batch care gap detection for {} patients", patientIds.size());

        patientIds.parallelStream()
            .forEach(this::detectCareGaps);

        log.info("Completed batch care gap detection");
    }

    /**
     * Detects gaps in preventive care
     */
    private List<CareGap> detectPreventiveCareGaps(Object patient, List<?> observations) {
        List<CareGap> gaps = new ArrayList<>();

        // Example: Annual wellness visit check
        boolean hasAnnualVisit = observations.stream()
            .anyMatch(obs -> "annual-wellness".equals(obs.toString())); // Simplified

        if (!hasAnnualVisit) {
            gaps.add(CareGap.builder()
                .patientId(patient.toString())
                .gapType("PREVENTIVE_CARE")
                .description("Annual wellness visit overdue")
                .priority("MEDIUM")
                .dueDate(LocalDateTime.now().plusDays(30))
                .build());
        }

        return gaps;
    }

    /**
     * Detects gaps in chronic disease management
     */
    private List<CareGap> detectChronicDiseaseGaps(Object patient, List<?> conditions,
                                                   List<?> observations) {
        List<CareGap> gaps = new ArrayList<>();

        // Check for diabetes management gaps
        boolean hasDiabetes = conditions.stream()
            .anyMatch(c -> c.toString().contains("diabetes"));

        if (hasDiabetes) {
            // Check for recent HbA1c test
            boolean hasRecentHbA1c = observations.stream()
                .anyMatch(obs -> obs.toString().contains("HbA1c"));

            if (!hasRecentHbA1c) {
                gaps.add(CareGap.builder()
                    .patientId(patient.toString())
                    .gapType("CHRONIC_DISEASE_MONITORING")
                    .description("HbA1c test overdue for diabetic patient")
                    .priority("HIGH")
                    .dueDate(LocalDateTime.now().plusDays(7))
                    .measureId("HEDIS-CDC")
                    .build());
            }
        }

        return gaps;
    }

    /**
     * Detects medication adherence gaps
     */
    private List<CareGap> detectMedicationAdherenceGaps(Object patient, List<?> medications) {
        List<CareGap> gaps = new ArrayList<>();

        // Check for medication refill gaps
        medications.forEach(med -> {
            // Simplified logic - in reality would check refill dates
            gaps.add(CareGap.builder()
                .patientId(patient.toString())
                .gapType("MEDICATION_ADHERENCE")
                .description("Medication refill needed: " + med.toString())
                .priority("MEDIUM")
                .dueDate(LocalDateTime.now().plusDays(14))
                .build());
        });

        return gaps;
    }

    /**
     * Detects screening gaps
     */
    private List<CareGap> detectScreeningGaps(Object patient, List<?> observations) {
        List<CareGap> gaps = new ArrayList<>();

        // Check for cancer screenings based on age/gender
        // This is simplified - real implementation would check demographics

        boolean hasColonoscopy = observations.stream()
            .anyMatch(obs -> obs.toString().contains("colonoscopy"));

        if (!hasColonoscopy) {
            gaps.add(CareGap.builder()
                .patientId(patient.toString())
                .gapType("CANCER_SCREENING")
                .description("Colorectal cancer screening overdue")
                .priority("HIGH")
                .dueDate(LocalDateTime.now().plusDays(60))
                .measureId("HEDIS-COL")
                .build());
        }

        return gaps;
    }

    /**
     * Closes a care gap when action is taken
     */
    public void closeCareGap(String gapId, String closureReason) {
        log.info("Closing care gap: {} with reason: {}", gapId, closureReason);

        var gap = careGapRepository.findById(gapId)
            .orElseThrow(() -> new IllegalArgumentException("Care gap not found: " + gapId));

        gap.setStatus("CLOSED");
        gap.setClosedDate(LocalDateTime.now());
        gap.setClosureReason(closureReason);

        careGapRepository.save(gap);

        // Notify quality measure service directly
        qualityMeasureService.recordGapClosure(gap.getPatientId(), gap.getMeasureId());
    }
}