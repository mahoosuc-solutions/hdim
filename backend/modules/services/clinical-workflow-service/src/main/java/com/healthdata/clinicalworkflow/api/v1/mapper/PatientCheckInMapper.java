package com.healthdata.clinicalworkflow.api.v1.mapper;

import com.healthdata.clinicalworkflow.api.v1.dto.CheckInHistoryResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.CheckInResponse;
import com.healthdata.clinicalworkflow.domain.model.PatientCheckInEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for converting PatientCheckInEntity to response DTOs.
 *
 * Handles type conversions:
 * - UUID → String (for patientId)
 * - Instant → LocalDateTime (for timestamps)
 * - Entity Boolean fields → Response Boolean fields
 *
 * HIPAA Compliance: No PHI caching - stateless mapper only
 * Multi-tenant: Preserves tenant_id for response filtering
 */
@Component
public class PatientCheckInMapper {

    /**
     * Convert PatientCheckInEntity to CheckInResponse DTO
     *
     * @param entity PatientCheckInEntity from database
     * @return CheckInResponse for API response
     */
    public CheckInResponse toCheckInResponse(PatientCheckInEntity entity) {
        if (entity == null) {
            return null;
        }

        return CheckInResponse.builder()
                .id(entity.getId())
                .patientId(uuidToString(entity.getPatientId()))
                .patientName(null)  // Populated by service layer if needed
                .appointmentId(entity.getAppointmentId())
                .checkInTime(instantToLocalDateTime(entity.getCheckInTime()))
                .insuranceVerified(entity.getInsuranceVerified())
                .consentSigned(entity.getConsentObtained())
                .demographicsConfirmed(entity.getDemographicsUpdated())
                .status(mapCheckInStatus(entity))
                .notes(entity.getNotes())
                .checkInMethod("FRONT_DESK")  // Default value - can be enhanced
                .tenantId(entity.getTenantId())
                .createdAt(instantToLocalDateTime(entity.getCreatedAt()))
                .updatedAt(instantToLocalDateTime(entity.getUpdatedAt()))
                .build();
    }

    /**
     * Convert list of PatientCheckInEntity to CheckInHistoryResponse
     *
     * @param entities List of check-in entities
     * @param totalRecords Total record count (from service/repository)
     * @param currentPage Current page number
     * @param pageSize Number of records per page
     * @return CheckInHistoryResponse with pagination metadata
     */
    public CheckInHistoryResponse toCheckInHistoryResponse(
            List<PatientCheckInEntity> entities,
            Long totalRecords,
            Integer currentPage,
            Integer pageSize) {

        if (entities == null) {
            entities = List.of();
        }

        List<CheckInResponse> checkIns = entities.stream()
                .map(this::toCheckInResponse)
                .toList();

        int totalPages = pageSize != null && pageSize > 0 && totalRecords != null
                ? (int) Math.ceil((double) totalRecords / pageSize)
                : 0;

        return CheckInHistoryResponse.builder()
                .checkIns(checkIns)
                .totalRecords(totalRecords != null ? totalRecords : 0L)
                .currentPage(currentPage != null ? currentPage : 0)
                .pageSize(pageSize != null ? pageSize : checkIns.size())
                .totalPages(totalPages)
                .build();
    }

    /**
     * Map entity status to API status
     * Entity status values: "checked-in", "in-progress", "completed"
     * API status values: "PENDING", "INCOMPLETE", "COMPLETE"
     *
     * @param entity Check-in entity
     * @return Mapped status string
     */
    private String mapCheckInStatus(PatientCheckInEntity entity) {
        if (entity.getInsuranceVerified() &&
            entity.getConsentObtained() &&
            entity.getDemographicsUpdated()) {
            return "COMPLETE";
        } else if (entity.getInsuranceVerified() ||
                   entity.getConsentObtained() ||
                   entity.getDemographicsUpdated()) {
            return "IN_PROGRESS";
        } else {
            return "PENDING";
        }
    }

    /**
     * Convert UUID to String with null safety
     *
     * @param uuid UUID value
     * @return String representation or null
     */
    private String uuidToString(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    /**
     * Convert Instant to LocalDateTime with null safety
     * Uses system default timezone
     *
     * @param instant Instant timestamp
     * @return LocalDateTime or null
     */
    private LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null
                ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                : null;
    }
}
