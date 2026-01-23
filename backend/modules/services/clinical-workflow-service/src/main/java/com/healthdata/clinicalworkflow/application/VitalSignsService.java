package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.api.v1.dto.VitalAlertResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.VitalSignsResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.VitalsHistoryResponse;
import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import com.healthdata.clinicalworkflow.domain.repository.VitalSignsRecordRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final com.healthdata.clinicalworkflow.domain.repository.RoomAssignmentRepository roomAssignmentRepository;
    private final com.healthdata.clinicalworkflow.client.PatientServiceClient patientServiceClient;

    /**
     * Record vital signs (internal version with UUID patientId)
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
     * 2a. Record vital signs adapter (for controller)
     *
     * Adapter method that converts controller request parameters to internal format.
     * Handles type conversions: String→UUID, Integer→BigDecimal, unit conversions
     * pounds→kg, inches→cm.
     *
     * @param tenantId the tenant ID
     * @param request vital signs request from controller (DTO)
     * @param userId the user recording vitals (for audit trail)
     * @return created vital signs record as DTO
     */
    @Transactional
    public VitalSignsResponse recordVitalSigns(
            String tenantId,
            com.healthdata.clinicalworkflow.api.v1.dto.VitalSignsRequest request,
            String userId) {
        log.debug("Recording vital signs for patient {} in tenant {}",
                request.getPatientId(), tenantId);

        // Convert request parameters to internal format
        UUID patientId;
        try {
            patientId = UUID.fromString(request.getPatientId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid patient ID format: " + request.getPatientId());
        }

        // Create internal request with converted types
        VitalSignsRequest internalRequest = VitalSignsRequest.builder()
                .patientId(patientId)
                .encounterId(request.getEncounterId())
                .recordedBy(userId)
                .systolicBp(request.getSystolicBP() != null ?
                        BigDecimal.valueOf(request.getSystolicBP()) : null)
                .diastolicBp(request.getDiastolicBP() != null ?
                        BigDecimal.valueOf(request.getDiastolicBP()) : null)
                .heartRate(request.getHeartRate() != null ?
                        BigDecimal.valueOf(request.getHeartRate()) : null)
                .temperatureF(request.getTemperature())
                .respirationRate(request.getRespiratoryRate() != null ?
                        BigDecimal.valueOf(request.getRespiratoryRate()) : null)
                .oxygenSaturation(request.getOxygenSaturation() != null ?
                        BigDecimal.valueOf(request.getOxygenSaturation()) : null)
                .weightKg(convertPoundsToKg(request.getWeight()))
                .heightCm(convertInchesToCm(request.getHeight()))
                .notes(request.getNotes())
                .build();

        VitalSignsRecordEntity entity = recordVitals(internalRequest, tenantId);
        return mapToVitalSignsResponse(entity);
    }

    /**
     * Convert pounds to kilograms
     *
     * @param pounds weight in pounds (BigDecimal from DTO)
     * @return weight in kilograms (rounded to 2 decimal places)
     */
    private BigDecimal convertPoundsToKg(BigDecimal pounds) {
        if (pounds == null) {
            return null;
        }
        return pounds.multiply(new BigDecimal("0.453592")).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convert inches to centimeters
     *
     * @param inches height in inches (BigDecimal from DTO)
     * @return height in centimeters (rounded to 2 decimal places)
     */
    private BigDecimal convertInchesToCm(BigDecimal inches) {
        if (inches == null) {
            return null;
        }
        return inches.multiply(new BigDecimal("2.54")).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convert kilograms to pounds
     *
     * @param kg weight in kilograms
     * @return weight in pounds (rounded to 2 decimal places)
     */
    private BigDecimal convertKgToPounds(BigDecimal kg) {
        if (kg == null) {
            return null;
        }
        return kg.divide(new BigDecimal("0.453592"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Convert centimeters to inches
     *
     * @param cm height in centimeters
     * @return height in inches (rounded to 2 decimal places)
     */
    private BigDecimal convertCmToInches(BigDecimal cm) {
        if (cm == null) {
            return null;
        }
        return cm.divide(new BigDecimal("2.54"), 2, RoundingMode.HALF_UP);
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
     * Get vitals alerts (internal version)
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
                .findByAlertStatusAndTenant("warning", tenantId);
        List<VitalSignsRecordEntity> critical = vitalsRepository
                .findByAlertStatusAndTenant("critical", tenantId);

        List<VitalSignsRecordEntity> allAlerts = new ArrayList<>();
        allAlerts.addAll(critical); // Critical first
        allAlerts.addAll(warnings);

        return allAlerts;
    }

    /**
     * 2d. Get vital alerts with acknowledged filter parameter (returns DTOs)
     *
     * Retrieves vital alerts with option to include/exclude acknowledged alerts.
     *
     * @param tenantId the tenant ID
     * @param includeAcknowledged whether to include acknowledged alerts
     * @return list of vital alerts as DTOs
     */
    public List<VitalAlertResponse> getVitalAlerts(
            String tenantId,
            boolean includeAcknowledged) {
        log.debug("Retrieving vital alerts for tenant {} (includeAcknowledged={})",
                tenantId, includeAcknowledged);

        List<VitalSignsRecordEntity> alerts = getVitalsAlerts(tenantId);

        // Filter out acknowledged alerts if requested
        if (!includeAcknowledged) {
            alerts = alerts.stream()
                    .filter(v -> v.getAcknowledgedBy() == null)
                    .collect(Collectors.toList());
        }

        return alerts.stream()
                .map(this::mapToVitalAlertResponse)
                .collect(Collectors.toList());
    }

    /**
     * 2f. Get critical alerts for tenant (returns DTOs)
     *
     * Retrieves only critical vital sign alerts.
     *
     * @param tenantId the tenant ID
     * @return list of critical alerts as DTOs sorted by recorded time descending
     */
    public List<VitalAlertResponse> getCriticalAlerts(String tenantId) {
        log.debug("Retrieving critical vital alerts for tenant {}", tenantId);

        List<VitalSignsRecordEntity> criticalAlerts = vitalsRepository
                .findByTenantIdAndAlertStatusOrderByRecordedAtDesc(tenantId, "critical");

        return criticalAlerts.stream()
                .map(this::mapToVitalAlertResponse)
                .collect(Collectors.toList());
    }

    /**
     * 2g. Acknowledge alert (returns DTO)
     *
     * Marks a vital signs alert as acknowledged by a user.
     * Updates audit trail with acknowledgement timestamp.
     *
     * @param tenantId the tenant ID
     * @param vitalsId the vitals ID
     * @param userId the user acknowledging the alert
     * @return updated vitals record as alert DTO
     */
    @Transactional
    public VitalAlertResponse acknowledgeAlert(
            String tenantId,
            UUID vitalsId,
            String userId) {
        log.debug("Acknowledging alert for vitals {} in tenant {} by user {}",
                vitalsId, tenantId, userId);

        VitalSignsRecordEntity vitals = getVitalsById(vitalsId, tenantId);

        // Set acknowledgement fields
        vitals.setAcknowledgedBy(userId);
        vitals.setAcknowledgedAt(Instant.now());

        VitalSignsRecordEntity updated = vitalsRepository.save(vitals);

        log.info("Alert acknowledged for vitals {} in tenant {} by user {}",
                vitalsId, tenantId, userId);

        return mapToVitalAlertResponse(updated);
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
     * Get latest vitals for patient (UUID patientId version)
     *
     * @param patientId the patient ID (UUID)
     * @param tenantId the tenant ID
     * @return latest vitals or empty
     */
    @Cacheable(value = "latestVitals", key = "#tenantId + ':' + #patientId")
    public Optional<VitalSignsRecordEntity> getLatestVitalsUUID(UUID patientId, String tenantId) {
        log.debug("Retrieving latest vitals for patient {} in tenant {}", patientId, tenantId);

        return vitalsRepository.findLatestVitalForPatient(patientId, tenantId);
    }

    /**
     * 2e. Get latest vitals for patient (returns DTO)
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID (String)
     * @return latest vitals record as DTO
     */
    public VitalSignsResponse getLatestVitals(String tenantId, String patientId) {
        log.debug("Retrieving latest vitals for patient {} in tenant {}", patientId, tenantId);

        UUID pid = UUID.fromString(patientId);
        VitalSignsRecordEntity entity = getLatestVitalsUUID(pid, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vital signs not found for patient: " + patientId));
        return mapToVitalSignsResponse(entity);
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
     * 2b. Get vital signs (returns DTO)
     *
     * @param tenantId the tenant ID
     * @param vitalsId the vitals ID
     * @return vitals record as DTO
     */
    public VitalSignsResponse getVitalSigns(String tenantId, UUID vitalsId) {
        log.debug("Retrieving vital signs {} in tenant {}", vitalsId, tenantId);
        VitalSignsRecordEntity entity = getVitalsById(vitalsId, tenantId);
        return mapToVitalSignsResponse(entity);
    }

    /**
     * 2c. Get vitals history with pagination support (returns DTO)
     *
     * Retrieves vitals for patient with pagination.
     * Converts String patientId to UUID.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID (String)
     * @param pageable pagination info (not yet fully implemented)
     * @return paginated vitals history as DTO
     */
    public VitalsHistoryResponse getVitalsHistory(
            String tenantId,
            String patientId,
            Pageable pageable) {
        log.debug("Retrieving vitals history for patient {} in tenant {}", patientId, tenantId);

        UUID pid = UUID.fromString(patientId);
        // TODO: Implement full pagination support
        // For now return all vitals sorted by recorded time descending
        List<VitalSignsRecordEntity> entities = getAllPatientVitals(pid, tenantId);
        List<VitalSignsResponse> vitals = entities.stream()
                .map(this::mapToVitalSignsResponse)
                .collect(Collectors.toList());

        return VitalsHistoryResponse.builder()
                .vitals(vitals)
                .totalRecords((long) vitals.size())
                .currentPage(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalPages((int) Math.ceil((double) vitals.size() / pageable.getPageSize()))
                .build();
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

    // ========== MAPPER METHODS ==========

    /**
     * Map VitalSignsRecordEntity to VitalSignsResponse DTO
     *
     * Converts internal entity to API response format.
     * Handles unit conversions: kg→pounds, cm→inches
     * Converts BigDecimal to Integer where appropriate
     *
     * @param entity the vitals entity
     * @return vitals response DTO
     */
    private VitalSignsResponse mapToVitalSignsResponse(VitalSignsRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        List<String> alerts = new ArrayList<>();
        if (entity.getAlertMessage() != null && !entity.getAlertMessage().isEmpty()) {
            alerts.add(entity.getAlertMessage());
        }

        return VitalSignsResponse.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId().toString())
                .patientName(resolvePatientName(entity.getPatientId(), entity.getTenantId()))
                .encounterId(entity.getEncounterId())
                .measuredAt(entity.getRecordedAt() != null ?
                        LocalDateTime.ofInstant(entity.getRecordedAt(), ZoneId.systemDefault()) : null)
                .systolicBP(entity.getSystolicBp() != null ? entity.getSystolicBp().intValue() : null)
                .diastolicBP(entity.getDiastolicBp() != null ? entity.getDiastolicBp().intValue() : null)
                .heartRate(entity.getHeartRate() != null ? entity.getHeartRate().intValue() : null)
                .respiratoryRate(entity.getRespirationRate() != null ? entity.getRespirationRate().intValue() : null)
                .temperature(entity.getTemperatureF())
                .oxygenSaturation(entity.getOxygenSaturation() != null ? entity.getOxygenSaturation().intValue() : null)
                .weight(convertKgToPounds(entity.getWeightKg()))
                .height(convertCmToInches(entity.getHeightCm()))
                .bmi(entity.getBmi())
                .painLevel(null) // Not tracked in entity yet
                .alerts(alerts)
                .hasCriticalAlerts("critical".equals(entity.getAlertStatus()))
                .notes(entity.getNotes())
                .tenantId(entity.getTenantId())
                .createdAt(entity.getCreatedAt() != null ?
                        LocalDateTime.ofInstant(entity.getCreatedAt(), ZoneId.systemDefault()) : null)
                .recordedBy(entity.getRecordedBy())
                .build();
    }

    /**
     * Map VitalSignsRecordEntity to VitalAlertResponse DTO
     *
     * Converts entity with alerts to alert-specific response format.
     *
     * @param entity the vitals entity
     * @return vital alert response DTO
     */
    private VitalAlertResponse mapToVitalAlertResponse(VitalSignsRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        return VitalAlertResponse.builder()
                .alertId(entity.getId()) // Using vitals ID as alert ID
                .vitalSignsRecordId(entity.getId())
                .patientId(entity.getPatientId().toString())
                .patientName(resolvePatientName(entity.getPatientId(), entity.getTenantId()))
                .roomNumber(resolveRoomNumber(entity.getPatientId(), entity.getTenantId()))
                .alertType(determineAlertType(entity))
                .severity(entity.getAlertStatus().toUpperCase())
                .message(entity.getAlertMessage())
                .measuredValue(extractMeasuredValue(entity))
                .normalRange(determineNormalRange(entity))
                .alertedAt(entity.getRecordedAt() != null ?
                        LocalDateTime.ofInstant(entity.getRecordedAt(), ZoneId.systemDefault()) : null)
                .acknowledged(entity.getAcknowledgedBy() != null)
                .acknowledgedAt(entity.getAcknowledgedAt() != null ?
                        LocalDateTime.ofInstant(entity.getAcknowledgedAt(), ZoneId.systemDefault()) : null)
                .acknowledgedBy(entity.getAcknowledgedBy())
                .build();
    }

    /**
     * Determine alert type from vital signs entity
     *
     * @param entity the vitals entity
     * @return alert type string
     */
    private String determineAlertType(VitalSignsRecordEntity entity) {
        if (entity.getAlertMessage() == null) {
            return "UNKNOWN";
        }
        String message = entity.getAlertMessage().toUpperCase();
        if (message.contains("SYSTOLIC BP") && (message.contains(">") || message.contains("HIGH"))) {
            return "HIGH_BLOOD_PRESSURE";
        } else if (message.contains("SYSTOLIC BP") && (message.contains("<") || message.contains("LOW"))) {
            return "LOW_BLOOD_PRESSURE";
        } else if (message.contains("HEART RATE") && (message.contains(">") || message.contains("HIGH"))) {
            return "HIGH_HEART_RATE";
        } else if (message.contains("HEART RATE") && (message.contains("<") || message.contains("LOW"))) {
            return "LOW_HEART_RATE";
        } else if (message.contains("TEMPERATURE") && message.contains(">")) {
            return "HIGH_TEMPERATURE";
        } else if (message.contains("TEMPERATURE") && message.contains("<")) {
            return "LOW_TEMPERATURE";
        } else if (message.contains("O2") || message.contains("OXYGEN")) {
            return "LOW_OXYGEN_SATURATION";
        }
        return "ABNORMAL_VITAL_SIGN";
    }

    /**
     * Extract measured value from alert message
     *
     * @param entity the vitals entity
     * @return measured value string
     */
    private String extractMeasuredValue(VitalSignsRecordEntity entity) {
        // This is a simplified extraction - in production would parse message more carefully
        if (entity.getAlertMessage() == null) {
            return "";
        }
        String message = entity.getAlertMessage();
        if (message.contains("Systolic BP")) {
            return entity.getSystolicBp() + " mmHg";
        } else if (message.contains("Heart Rate")) {
            return entity.getHeartRate() + " bpm";
        } else if (message.contains("O2")) {
            return entity.getOxygenSaturation() + "%";
        } else if (message.contains("Temperature")) {
            return entity.getTemperatureF() + "°F";
        }
        return "";
    }

    /**
     * Determine normal range based on alert type
     *
     * @param entity the vitals entity
     * @return normal range string
     */
    private String determineNormalRange(VitalSignsRecordEntity entity) {
        if (entity.getAlertMessage() == null) {
            return "";
        }
        String message = entity.getAlertMessage();
        if (message.contains("Systolic BP")) {
            return "90-140 mmHg";
        } else if (message.contains("Heart Rate")) {
            return "60-100 bpm";
        } else if (message.contains("O2")) {
            return "95-100%";
        } else if (message.contains("Temperature")) {
            return "97.0-99.0°F";
        }
        return "";
    }

    /**
     * Resolve room number for patient
     *
     * Queries Room Assignment Repository to find the active room assignment for the patient.
     * Uses circuit breaker pattern for resilience - returns null if service is unavailable.
     * Room mappings are cached with 5-minute TTL (HIPAA compliant for PHI).
     *
     * Critical for emergency response: Providers need to know which room to respond to
     * when vital sign alerts are triggered.
     *
     * @param patientId the patient UUID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return room number (e.g., "EXAM-101") or null if not found/error
     */
    @Cacheable(value = "patientRoomMapping", key = "#tenantId + ':' + #patientId")
    private String resolveRoomNumber(UUID patientId, String tenantId) {
        try {
            log.debug("Resolving room number for patient {} in tenant {}", patientId, tenantId);

            // Query active room assignment (status = 'occupied' or 'reserved')
            Optional<com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity> activeRoom =
                roomAssignmentRepository.findActiveRoomForPatient(tenantId, patientId);

            if (activeRoom.isPresent()) {
                String roomNumber = activeRoom.get().getRoomNumber();
                log.debug("Resolved room number {} for patient {}", roomNumber, patientId);
                return roomNumber;
            }

            log.debug("No active room found for patient {} in tenant {}", patientId, tenantId);
            return null;

        } catch (Exception e) {
            // Circuit breaker: Log error but don't fail the alert
            // This ensures vital sign alerts are still delivered even if room lookup fails
            log.warn("Failed to resolve room number for patient {} in tenant {}: {}",
                patientId, tenantId, e.getMessage());
            return null;
        }
    }

    /**
     * Resolve patient name from Patient Service
     *
     * Fetches patient demographic information from Patient Service to display
     * human-readable names in vital sign alerts. Uses circuit breaker pattern
     * for resilience - returns null if service unavailable.
     *
     * Patient names are cached with 5-minute TTL (HIPAA compliant for PHI).
     *
     * Critical for clinical workflows: Providers need to know which patient
     * has abnormal vital signs to prioritize response.
     *
     * @param patientId the patient UUID
     * @param tenantId the tenant ID for multi-tenant isolation
     * @return formatted patient name (e.g., "Smith, John") or null if unavailable
     */
    private String resolvePatientName(UUID patientId, String tenantId) {
        try {
            log.debug("Resolving patient name for patient {} in tenant {}", patientId, tenantId);

            com.healthdata.clinicalworkflow.client.dto.PatientDTO patient =
                patientServiceClient.getPatient(patientId, tenantId);

            if (patient != null) {
                String formattedName = patient.getFormattedName();
                log.debug("Resolved patient name '{}' for patient {}", formattedName, patientId);
                return formattedName;
            }

            log.debug("Patient {} not found in Patient Service for tenant {}", patientId, tenantId);
            return null;

        } catch (Exception e) {
            // Circuit breaker: Log error but don't fail the alert
            // This ensures vital sign alerts are still delivered even if patient lookup fails
            log.warn("Failed to resolve patient name for patient {} in tenant {}: {}",
                patientId, tenantId, e.getMessage());
            return null;
        }
    }

    /**
     * Vital Signs Request DTO (Internal)
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
