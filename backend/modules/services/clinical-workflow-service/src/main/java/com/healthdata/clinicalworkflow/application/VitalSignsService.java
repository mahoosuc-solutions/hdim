package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import com.healthdata.clinicalworkflow.domain.repository.VitalSignsRecordRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vital Signs Service
 *
 * Manages vital signs recording and alert detection for Medical Assistants:
 * - Record vital signs (BP, HR, Temp, O2, RR, Weight, Height)
 * - Automatic abnormal value detection
 * - Alert generation (warning, critical)
 * - BMI calculation
 * - FHIR Observation resource creation
 *
 * Alert Thresholds:
 * - Systolic BP: >180 (critical), >140 (warning), <70 (warning), <60 (critical)
 * - Heart Rate: >130 (critical), >100 (warning), <50 (warning), <40 (critical)
 * - O2 Saturation: <85% (critical), <90% (warning)
 *
 * HIPAA Compliance:
 * - All methods enforce multi-tenant isolation
 * - Audit logging via @Audited annotations (to be added at controller level)
 * - Cache TTL <= 5 minutes for PHI data
 *
 * Integration Points:
 * - FHIR Observation: Creates observation resources
 * - FHIR Encounter: Links vitals to encounter
 * - Alert Service: Sends alerts to providers for critical values
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VitalSignsService {

    private final VitalSignsRecordRepository vitalsRepository;

    /**
     * Record vital signs
     *
     * Main workflow for recording patient vital signs. Automatically
     * detects abnormal values and triggers alerts.
     *
     * @param request vital signs request
     * @param tenantId the tenant ID (HIPAA §164.312(d))
     * @return created vital signs record
     */
    @Transactional
    public VitalSignsRecordEntity recordVitals(VitalSignsRequest request, String tenantId) {
        log.debug("Recording vitals for patient {} in tenant {}",
                request.getPatientId(), tenantId);

        // Calculate BMI if weight and height provided
        BigDecimal bmi = null;
        if (request.getWeightKg() != null && request.getHeightCm() != null) {
            bmi = calculateBMI(request.getWeightKg(), request.getHeightCm());
        }

        VitalSignsRecordEntity vitals = VitalSignsRecordEntity.builder()
                .tenantId(tenantId)
                .patientId(request.getPatientId())
                .encounterId(request.getEncounterId())
                .recordedBy(request.getRecordedBy() != null ? request.getRecordedBy() : "system")
                .systolicBp(request.getSystolicBp())
                .diastolicBp(request.getDiastolicBp())
                .heartRate(request.getHeartRate())
                .temperatureF(request.getTemperatureF())
                .respirationRate(request.getRespirationRate())
                .oxygenSaturation(request.getOxygenSaturation())
                .weightKg(request.getWeightKg())
                .heightCm(request.getHeightCm())
                .bmi(bmi)
                .recordedAt(Instant.now())
                .notes(request.getNotes())
                .alertStatus("normal") // Will be updated by detectAbnormalValues
                .build();

        // Detect abnormal values and set alert status
        detectAbnormalValues(vitals);

        VitalSignsRecordEntity saved = vitalsRepository.save(vitals);

        log.info("Vitals recorded: {} for patient {} with alert status: {}",
                saved.getId(), request.getPatientId(), saved.getAlertStatus());

        // Trigger alerts if abnormal
        if (!"normal".equals(saved.getAlertStatus())) {
            triggerAlerts(saved, tenantId);
        }

        return saved;
    }

    /**
     * Detect abnormal values
     *
     * Analyzes vital signs and sets alert status and message.
     * Implements clinical decision rules for vital sign thresholds.
     *
     * @param vitals the vital signs record to analyze
     */
    public void detectAbnormalValues(VitalSignsRecordEntity vitals) {
        List<String> alerts = new ArrayList<>();
        String highestSeverity = "normal";

        // Systolic BP checks
        if (vitals.getSystolicBp() != null) {
            BigDecimal systolic = vitals.getSystolicBp();
            if (systolic.compareTo(new BigDecimal("180")) > 0) {
                alerts.add("CRITICAL: Systolic BP " + systolic + " mmHg (>180)");
                highestSeverity = "critical";
            } else if (systolic.compareTo(new BigDecimal("140")) > 0) {
                alerts.add("WARNING: Systolic BP " + systolic + " mmHg (>140)");
                if (!"critical".equals(highestSeverity)) {
                    highestSeverity = "warning";
                }
            } else if (systolic.compareTo(new BigDecimal("60")) < 0) {
                alerts.add("CRITICAL: Systolic BP " + systolic + " mmHg (<60)");
                highestSeverity = "critical";
            } else if (systolic.compareTo(new BigDecimal("70")) < 0) {
                alerts.add("WARNING: Systolic BP " + systolic + " mmHg (<70)");
                if (!"critical".equals(highestSeverity)) {
                    highestSeverity = "warning";
                }
            }
        }

        // Heart Rate checks
        if (vitals.getHeartRate() != null) {
            BigDecimal hr = vitals.getHeartRate();
            if (hr.compareTo(new BigDecimal("130")) > 0) {
                alerts.add("CRITICAL: Heart Rate " + hr + " bpm (>130)");
                highestSeverity = "critical";
            } else if (hr.compareTo(new BigDecimal("100")) > 0) {
                alerts.add("WARNING: Heart Rate " + hr + " bpm (>100)");
                if (!"critical".equals(highestSeverity)) {
                    highestSeverity = "warning";
                }
            } else if (hr.compareTo(new BigDecimal("40")) < 0) {
                alerts.add("CRITICAL: Heart Rate " + hr + " bpm (<40)");
                highestSeverity = "critical";
            } else if (hr.compareTo(new BigDecimal("50")) < 0) {
                alerts.add("WARNING: Heart Rate " + hr + " bpm (<50)");
                if (!"critical".equals(highestSeverity)) {
                    highestSeverity = "warning";
                }
            }
        }

        // O2 Saturation checks
        if (vitals.getOxygenSaturation() != null) {
            BigDecimal o2 = vitals.getOxygenSaturation();
            if (o2.compareTo(new BigDecimal("85")) < 0) {
                alerts.add("CRITICAL: O2 Saturation " + o2 + "% (<85%)");
                highestSeverity = "critical";
            } else if (o2.compareTo(new BigDecimal("90")) < 0) {
                alerts.add("WARNING: O2 Saturation " + o2 + "% (<90%)");
                if (!"critical".equals(highestSeverity)) {
                    highestSeverity = "warning";
                }
            }
        }

        // Temperature checks (optional - not in spec but clinically relevant)
        if (vitals.getTemperatureF() != null) {
            BigDecimal temp = vitals.getTemperatureF();
            if (temp.compareTo(new BigDecimal("104")) > 0) {
                alerts.add("CRITICAL: Temperature " + temp + "°F (>104°F)");
                highestSeverity = "critical";
            } else if (temp.compareTo(new BigDecimal("100.4")) > 0) {
                alerts.add("WARNING: Temperature " + temp + "°F (>100.4°F - Fever)");
                if (!"critical".equals(highestSeverity)) {
                    highestSeverity = "warning";
                }
            } else if (temp.compareTo(new BigDecimal("95")) < 0) {
                alerts.add("CRITICAL: Temperature " + temp + "°F (<95°F - Hypothermia)");
                highestSeverity = "critical";
            }
        }

        vitals.setAlertStatus(highestSeverity);
        if (!alerts.isEmpty()) {
            vitals.setAlertMessage(String.join("; ", alerts));
        }
    }

    /**
     * Trigger alerts
     *
     * Sends alerts to providers for abnormal vital signs.
     * In production, this would publish to Kafka or send notifications.
     *
     * @param vitals the vital signs with alerts
     * @param tenantId the tenant ID
     */
    public void triggerAlerts(VitalSignsRecordEntity vitals, String tenantId) {
        log.warn("ALERT [{}]: Patient {} - {}",
                vitals.getAlertStatus().toUpperCase(),
                vitals.getPatientId(),
                vitals.getAlertMessage());

        // TODO: In production, publish to Kafka topic: vitals.alert.{severity}
        // TODO: Send real-time notification to provider via WebSocket
        // TODO: Create FHIR Flag resource for abnormal vitals
    }

    /**
     * Calculate BMI
     *
     * BMI = weight (kg) / (height (m))^2
     *
     * @param weightKg weight in kilograms
     * @param heightCm height in centimeters
     * @return BMI value
     */
    public BigDecimal calculateBMI(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null || heightCm == null ||
                weightKg.compareTo(BigDecimal.ZERO) <= 0 ||
                heightCm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight and height must be positive values");
        }

        // Convert height from cm to meters
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        // Calculate BMI
        BigDecimal bmi = weightKg.divide(
                heightM.multiply(heightM),
                2,
                RoundingMode.HALF_UP
        );

        return bmi;
    }

    /**
     * Get vitals alerts
     *
     * Retrieves all vitals with warning or critical alerts for tenant.
     *
     * @param tenantId the tenant ID
     * @return list of vitals with alerts
     */
    @Cacheable(value = "vitalsAlerts", key = "#tenantId")
    public List<VitalSignsRecordEntity> getVitalsAlerts(String tenantId) {
        log.debug("Retrieving vitals alerts for tenant {}", tenantId);

        List<VitalSignsRecordEntity> warnings = vitalsRepository
                .findByTenantIdAndAlertStatusOrderByRecordedAtDesc(tenantId, "warning");
        List<VitalSignsRecordEntity> critical = vitalsRepository
                .findByTenantIdAndAlertStatusOrderByRecordedAtDesc(tenantId, "critical");

        List<VitalSignsRecordEntity> allAlerts = new ArrayList<>();
        allAlerts.addAll(critical); // Critical first
        allAlerts.addAll(warnings);

        return allAlerts;
    }

    /**
     * Get patient vitals history
     *
     * Retrieves vitals for patient within date range.
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @param from start date
     * @param to end date
     * @return list of vitals
     */
    public List<VitalSignsRecordEntity> getPatientVitalsHistory(
            UUID patientId, String tenantId, LocalDateTime from, LocalDateTime to) {
        log.debug("Retrieving vitals history for patient {} from {} to {} in tenant {}",
                patientId, from, to, tenantId);

        return vitalsRepository.findPatientVitalsHistory(patientId, tenantId, from, to);
    }

    /**
     * Create FHIR Observation resource
     *
     * Converts vital signs to FHIR R4 Observation resource.
     * In production, this would call FHIR service API.
     *
     * @param vitals the vital signs record
     * @return FHIR Observation JSON (placeholder)
     */
    public String createObservationResource(VitalSignsRecordEntity vitals) {
        log.debug("Creating FHIR Observation for vitals {}", vitals.getId());

        // TODO: In production, use HAPI FHIR to create Observation resource
        // TODO: Call FHIR service to persist Observation
        // For now, return placeholder JSON structure

        return String.format(
                "{\"resourceType\": \"Observation\", \"id\": \"%s\", " +
                "\"status\": \"final\", \"code\": {\"text\": \"Vital Signs\"}, " +
                "\"subject\": {\"reference\": \"Patient/%s\"}, " +
                "\"effectiveDateTime\": \"%s\"}",
                vitals.getId(),
                vitals.getPatientId(),
                vitals.getRecordedAt()
        );
    }

    /**
     * Get latest vitals for patient
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return latest vitals or empty
     */
    @Cacheable(value = "latestVitals", key = "#tenantId + ':' + #patientId")
    public Optional<VitalSignsRecordEntity> getLatestVitals(UUID patientId, String tenantId) {
        log.debug("Retrieving latest vitals for patient {} in tenant {}", patientId, tenantId);

        return vitalsRepository.findLatestVitalForPatient(patientId, tenantId);
    }

    /**
     * Get vitals by ID
     *
     * @param vitalsId the vitals ID
     * @param tenantId the tenant ID
     * @return vitals record
     */
    public VitalSignsRecordEntity getVitalsById(UUID vitalsId, String tenantId) {
        log.debug("Retrieving vitals {} in tenant {}", vitalsId, tenantId);

        return vitalsRepository.findByIdAndTenantId(vitalsId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vitals not found: " + vitalsId));
    }

    /**
     * Get vitals for encounter
     *
     * @param encounterId the encounter ID
     * @param tenantId the tenant ID
     * @return list of vitals for encounter
     */
    public List<VitalSignsRecordEntity> getVitalsForEncounter(String encounterId, String tenantId) {
        log.debug("Retrieving vitals for encounter {} in tenant {}", encounterId, tenantId);

        return vitalsRepository.findByTenantIdAndEncounterIdOrderByRecordedAtDesc(
                tenantId, encounterId);
    }

    /**
     * Get vitals recorded by staff
     *
     * @param recordedBy staff identifier
     * @param tenantId the tenant ID
     * @return list of vitals recorded by staff
     */
    public List<VitalSignsRecordEntity> getVitalsByRecordedBy(String recordedBy, String tenantId) {
        log.debug("Retrieving vitals recorded by {} in tenant {}", recordedBy, tenantId);

        return vitalsRepository.findByTenantIdAndRecordedByOrderByRecordedAtDesc(
                tenantId, recordedBy);
    }

    /**
     * Get all vitals for patient (no date filter)
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return list of all vitals for patient
     */
    public List<VitalSignsRecordEntity> getAllPatientVitals(UUID patientId, String tenantId) {
        log.debug("Retrieving all vitals for patient {} in tenant {}", patientId, tenantId);

        return vitalsRepository.findByTenantIdAndPatientIdOrderByRecordedAtDesc(
                tenantId, patientId);
    }

    /**
     * Count critical alerts
     *
     * @param tenantId the tenant ID
     * @return count of critical vitals
     */
    public long countCriticalAlerts(String tenantId) {
        return vitalsRepository.countCriticalAlertsByTenant(tenantId);
    }

    /**
     * Update vitals notes
     *
     * @param vitalsId the vitals ID
     * @param notes the notes to add
     * @param tenantId the tenant ID
     * @return updated vitals
     */
    @Transactional
    public VitalSignsRecordEntity updateNotes(UUID vitalsId, String notes, String tenantId) {
        log.debug("Updating notes for vitals {} in tenant {}", vitalsId, tenantId);

        VitalSignsRecordEntity vitals = getVitalsById(vitalsId, tenantId);
        vitals.setNotes(notes);

        VitalSignsRecordEntity updated = vitalsRepository.save(vitals);

        log.info("Notes updated for vitals {} in tenant {}", vitalsId, tenantId);

        return updated;
    }

    /**
     * Vital Signs Request DTO
     */
    @Data
    @Builder
    public static class VitalSignsRequest {
        private UUID patientId;
        private String encounterId;
        private String recordedBy;
        private BigDecimal systolicBp;
        private BigDecimal diastolicBp;
        private BigDecimal heartRate;
        private BigDecimal temperatureF;
        private BigDecimal respirationRate;
        private BigDecimal oxygenSaturation;
        private BigDecimal weightKg;
        private BigDecimal heightCm;
        private String notes;
    }
}
