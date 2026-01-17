package com.healthdata.clinicalworkflow.api.v1.mapper;

import com.healthdata.clinicalworkflow.api.v1.dto.VitalAlertResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.VitalSignsResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.VitalsHistoryResponse;
import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for converting VitalSignsRecordEntity to response DTOs.
 *
 * Handles type conversions:
 * - UUID → String (for patientId)
 * - Instant → LocalDateTime (for timestamps)
 * - BigDecimal → Integer (for vital measurements like BP, HR, O2)
 * - BigDecimal → BigDecimal (for temperature, weight, height, BMI - precision retained)
 * - Alert status calculation from entity fields
 *
 * HIPAA Compliance: No PHI caching - stateless mapper only
 * Multi-tenant: Preserves tenant_id for response filtering
 */
@Component
public class VitalSignsMapper {

    /**
     * Convert VitalSignsRecordEntity to VitalSignsResponse DTO
     *
     * @param entity VitalSignsRecordEntity from database
     * @return VitalSignsResponse for API response
     */
    public VitalSignsResponse toVitalSignsResponse(VitalSignsRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        List<String> alerts = buildAlertsList(entity);
        boolean hasCriticalAlerts = "critical".equalsIgnoreCase(entity.getAlertStatus());

        return VitalSignsResponse.builder()
                .id(entity.getId())
                .patientId(uuidToString(entity.getPatientId()))
                .patientName(null)  // Populated by service layer if needed
                .encounterId(entity.getEncounterId())
                .measuredAt(instantToLocalDateTime(entity.getRecordedAt()))
                .systolicBP(bigDecimalToInteger(entity.getSystolicBp()))
                .diastolicBP(bigDecimalToInteger(entity.getDiastolicBp()))
                .heartRate(bigDecimalToInteger(entity.getHeartRate()))
                .respiratoryRate(bigDecimalToInteger(entity.getRespirationRate()))
                .temperature(entity.getTemperatureF())  // Keep precision
                .oxygenSaturation(bigDecimalToInteger(entity.getOxygenSaturation()))
                .weight(convertKgToLbs(entity.getWeightKg()))  // Convert kg to lbs
                .height(convertCmToInches(entity.getHeightCm()))  // Convert cm to inches
                .bmi(entity.getBmi())  // Keep precision
                .painLevel(null)  // Not in entity - can be added if needed
                .alerts(alerts)
                .hasCriticalAlerts(hasCriticalAlerts)
                .notes(entity.getNotes())
                .tenantId(entity.getTenantId())
                .createdAt(instantToLocalDateTime(entity.getCreatedAt()))
                .recordedBy(entity.getRecordedBy())
                .build();
    }

    /**
     * Convert list of VitalSignsRecordEntity to VitalsHistoryResponse with pagination
     *
     * @param entities List of vital signs entities
     * @param totalRecords Total record count (from service/repository)
     * @param currentPage Current page number
     * @param pageSize Number of records per page
     * @return VitalsHistoryResponse with pagination metadata
     */
    public VitalsHistoryResponse toVitalsHistoryResponse(
            List<VitalSignsRecordEntity> entities,
            Long totalRecords,
            Integer currentPage,
            Integer pageSize) {

        if (entities == null) {
            entities = List.of();
        }

        List<VitalSignsResponse> vitals = entities.stream()
                .map(this::toVitalSignsResponse)
                .toList();

        int totalPages = pageSize != null && pageSize > 0 && totalRecords != null
                ? (int) Math.ceil((double) totalRecords / pageSize)
                : 0;

        return VitalsHistoryResponse.builder()
                .vitals(vitals)
                .totalRecords(totalRecords != null ? totalRecords : 0L)
                .currentPage(currentPage != null ? currentPage : 0)
                .pageSize(pageSize != null ? pageSize : vitals.size())
                .totalPages(totalPages)
                .build();
    }

    /**
     * Convert VitalSignsRecordEntity to VitalAlertResponse
     * Used for critical alerts requiring immediate attention
     *
     * @param entity VitalSignsRecordEntity with alert status
     * @return VitalAlertResponse with alert details
     */
    public VitalAlertResponse toVitalAlertResponse(VitalSignsRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        // Determine primary alert type based on abnormal values
        String alertType = determinePrimaryAlertType(entity);
        String measuredValue = getMeasuredValueForAlert(entity, alertType);
        String normalRange = getNormalRangeForAlert(alertType);

        return VitalAlertResponse.builder()
                .alertId(entity.getId())  // Using vitals record ID as alert ID
                .vitalSignsRecordId(entity.getId())
                .patientId(uuidToString(entity.getPatientId()))
                .patientName(null)  // Populated by service layer
                .roomNumber(null)  // Populated by service layer if room assigned
                .alertType(alertType)
                .severity(mapAlertSeverity(entity.getAlertStatus()))
                .message(entity.getAlertMessage() != null
                        ? entity.getAlertMessage()
                        : "Abnormal vital signs detected")
                .measuredValue(measuredValue)
                .normalRange(normalRange)
                .alertedAt(instantToLocalDateTime(entity.getRecordedAt()))
                .acknowledged(entity.getAcknowledgedBy() != null)
                .acknowledgedAt(instantToLocalDateTime(entity.getAcknowledgedAt()))
                .acknowledgedBy(entity.getAcknowledgedBy())
                .build();
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Build list of alert messages for abnormal vitals
     */
    private List<String> buildAlertsList(VitalSignsRecordEntity entity) {
        List<String> alerts = new ArrayList<>();

        // Blood Pressure alerts
        if (entity.getSystolicBp() != null && entity.getSystolicBp().compareTo(new BigDecimal("140")) > 0) {
            alerts.add("High systolic blood pressure: " + entity.getSystolicBp() + " mmHg");
        }
        if (entity.getSystolicBp() != null && entity.getSystolicBp().compareTo(new BigDecimal("90")) < 0) {
            alerts.add("Low systolic blood pressure: " + entity.getSystolicBp() + " mmHg");
        }

        // Heart Rate alerts
        if (entity.getHeartRate() != null && entity.getHeartRate().compareTo(new BigDecimal("100")) > 0) {
            alerts.add("High heart rate: " + entity.getHeartRate() + " bpm");
        }
        if (entity.getHeartRate() != null && entity.getHeartRate().compareTo(new BigDecimal("60")) < 0) {
            alerts.add("Low heart rate: " + entity.getHeartRate() + " bpm");
        }

        // Temperature alerts
        if (entity.getTemperatureF() != null && entity.getTemperatureF().compareTo(new BigDecimal("100.4")) > 0) {
            alerts.add("Elevated temperature: " + entity.getTemperatureF() + " °F");
        }

        // Oxygen Saturation alerts
        if (entity.getOxygenSaturation() != null && entity.getOxygenSaturation().compareTo(new BigDecimal("95")) < 0) {
            alerts.add("Low oxygen saturation: " + entity.getOxygenSaturation() + "%");
        }

        return alerts;
    }

    /**
     * Determine primary alert type for VitalAlertResponse
     */
    private String determinePrimaryAlertType(VitalSignsRecordEntity entity) {
        // Priority order: O2 sat → BP → Heart Rate → Temp
        if (entity.getOxygenSaturation() != null && entity.getOxygenSaturation().compareTo(new BigDecimal("95")) < 0) {
            return "LOW_OXYGEN_SATURATION";
        }
        if (entity.getSystolicBp() != null && entity.getSystolicBp().compareTo(new BigDecimal("140")) > 0) {
            return "HIGH_BLOOD_PRESSURE";
        }
        if (entity.getSystolicBp() != null && entity.getSystolicBp().compareTo(new BigDecimal("90")) < 0) {
            return "LOW_BLOOD_PRESSURE";
        }
        if (entity.getHeartRate() != null && entity.getHeartRate().compareTo(new BigDecimal("100")) > 0) {
            return "HIGH_HEART_RATE";
        }
        if (entity.getHeartRate() != null && entity.getHeartRate().compareTo(new BigDecimal("60")) < 0) {
            return "LOW_HEART_RATE";
        }
        if (entity.getTemperatureF() != null && entity.getTemperatureF().compareTo(new BigDecimal("100.4")) > 0) {
            return "HIGH_TEMPERATURE";
        }
        return "ABNORMAL_VITALS";
    }

    /**
     * Get measured value string for specific alert type
     */
    private String getMeasuredValueForAlert(VitalSignsRecordEntity entity, String alertType) {
        return switch (alertType) {
            case "HIGH_BLOOD_PRESSURE", "LOW_BLOOD_PRESSURE" ->
                    entity.getSystolicBp() + "/" + entity.getDiastolicBp() + " mmHg";
            case "HIGH_HEART_RATE", "LOW_HEART_RATE" ->
                    entity.getHeartRate() + " bpm";
            case "HIGH_TEMPERATURE", "LOW_TEMPERATURE" ->
                    entity.getTemperatureF() + " °F";
            case "LOW_OXYGEN_SATURATION" ->
                    entity.getOxygenSaturation() + "%";
            default -> "See vitals record";
        };
    }

    /**
     * Get normal range for alert type
     */
    private String getNormalRangeForAlert(String alertType) {
        return switch (alertType) {
            case "HIGH_BLOOD_PRESSURE", "LOW_BLOOD_PRESSURE" -> "90-140/60-90 mmHg";
            case "HIGH_HEART_RATE", "LOW_HEART_RATE" -> "60-100 bpm";
            case "HIGH_TEMPERATURE", "LOW_TEMPERATURE" -> "97.0-100.4 °F";
            case "LOW_OXYGEN_SATURATION" -> "95-100%";
            default -> "See clinical guidelines";
        };
    }

    /**
     * Map entity alert status to API severity level
     */
    private String mapAlertSeverity(String alertStatus) {
        if (alertStatus == null) {
            return "NORMAL";
        }
        return switch (alertStatus.toLowerCase()) {
            case "critical" -> "CRITICAL";
            case "warning" -> "WARNING";
            default -> "NORMAL";
        };
    }

    /**
     * Convert weight from kilograms to pounds
     */
    private BigDecimal convertKgToLbs(BigDecimal kg) {
        if (kg == null) {
            return null;
        }
        return kg.multiply(new BigDecimal("2.20462")).setScale(1, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Convert height from centimeters to inches
     */
    private BigDecimal convertCmToInches(BigDecimal cm) {
        if (cm == null) {
            return null;
        }
        return cm.multiply(new BigDecimal("0.393701")).setScale(1, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Convert BigDecimal to Integer with null safety
     * Used for vital measurements that don't need decimal precision
     */
    private Integer bigDecimalToInteger(BigDecimal value) {
        return value != null ? value.intValue() : null;
    }

    /**
     * Convert UUID to String with null safety
     */
    private String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    /**
     * Convert Instant to LocalDateTime with null safety
     * Uses system default timezone
     */
    private LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null
                ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                : null;
    }
}
