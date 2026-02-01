package com.healthdata.clinicalworkflow.api.v1.mapper;

import com.healthdata.clinicalworkflow.api.v1.dto.RoomBoardResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.RoomStatusResponse;
import com.healthdata.clinicalworkflow.domain.model.RoomAssignmentEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for converting RoomAssignmentEntity to response DTOs.
 *
 * Handles type conversions:
 * - UUID → String (for patientId)
 * - Instant → LocalDateTime (for timestamps)
 * - Entity room status → API room status (mapping)
 * - Wait time calculation from assignedAt timestamp
 *
 * HIPAA Compliance: No PHI caching - stateless mapper only
 * Multi-tenant: Preserves tenant_id for response filtering
 */
@Component
public class RoomAssignmentMapper {

    /**
     * Convert RoomAssignmentEntity to RoomStatusResponse DTO
     *
     * @param entity RoomAssignmentEntity from database
     * @return RoomStatusResponse for API response
     */
    public RoomStatusResponse toRoomStatusResponse(RoomAssignmentEntity entity) {
        if (entity == null) {
            return null;
        }

        Integer waitTimeMinutes = calculateWaitTime(entity);
        String apiStatus = mapRoomStatus(entity.getStatus());

        return RoomStatusResponse.builder()
                .id(entity.getId())
                .roomNumber(entity.getRoomNumber())
                .status(apiStatus)
                .patientId(uuidToString(entity.getPatientId()))
                .patientName(null)  // Populated by service layer if needed
                .encounterId(entity.getEncounterId())
                .providerId(null)  // Populated by service layer if needed
                .providerName(null)  // Populated by service layer if needed
                .priority("ROUTINE")  // Default - can be enhanced with actual priority
                .assignedAt(instantToLocalDateTime(entity.getAssignedAt()))
                .waitTimeMinutes(waitTimeMinutes)
                .visitType(null)  // Can be populated from appointment if needed
                .notes(entity.getNotes())
                .tenantId(entity.getTenantId())
                .updatedAt(instantToLocalDateTime(entity.getUpdatedAt()))
                .build();
    }

    /**
     * Convert list of RoomAssignmentEntity to RoomBoardResponse
     * Aggregates room statuses and calculates counts
     *
     * @param entities List of room assignment entities
     * @return RoomBoardResponse with room board overview
     */
    public RoomBoardResponse toRoomBoardResponse(List<RoomAssignmentEntity> entities) {
        if (entities == null) {
            entities = List.of();
        }

        List<RoomStatusResponse> rooms = entities.stream()
                .map(this::toRoomStatusResponse)
                .toList();

        // Calculate status counts
        int availableCount = 0;
        int occupiedCount = 0;
        int cleaningCount = 0;
        int outOfServiceCount = 0;

        for (RoomAssignmentEntity entity : entities) {
            String status = entity.getStatus() != null ? entity.getStatus().toLowerCase() : "";
            switch (status) {
                case "available" -> availableCount++;
                case "occupied" -> occupiedCount++;
                case "cleaning" -> cleaningCount++;
                case "reserved" -> occupiedCount++;  // Reserved counts as occupied
                default -> outOfServiceCount++;
            }
        }

        return RoomBoardResponse.builder()
                .rooms(rooms)
                .availableCount(availableCount)
                .occupiedCount(occupiedCount)
                .cleaningCount(cleaningCount)
                .outOfServiceCount(outOfServiceCount)
                .totalRooms(rooms.size())
                .build();
    }

    /**
     * Convert list of available room entities to list of RoomStatusResponse
     *
     * @param entities List of available room entities
     * @return List of RoomStatusResponse
     */
    public List<RoomStatusResponse> toAvailableRoomsResponse(List<RoomAssignmentEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .filter(entity -> "available".equalsIgnoreCase(entity.getStatus()))
                .map(this::toRoomStatusResponse)
                .toList();
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Map entity room status to API status enum
     * Entity: available, occupied, cleaning, reserved
     * API: AVAILABLE, OCCUPIED, CLEANING, OUT_OF_SERVICE
     */
    private String mapRoomStatus(String entityStatus) {
        if (entityStatus == null) {
            return "OUT_OF_SERVICE";
        }

        return switch (entityStatus.toLowerCase()) {
            case "available" -> "AVAILABLE";
            case "occupied" -> "OCCUPIED";
            case "cleaning" -> "CLEANING";
            case "reserved" -> "OCCUPIED";  // Reserved treated as occupied
            default -> "OUT_OF_SERVICE";
        };
    }

    /**
     * Calculate wait time in minutes since room assignment
     *
     * @param entity Room assignment entity
     * @return Wait time in minutes or null if not applicable
     */
    private Integer calculateWaitTime(RoomAssignmentEntity entity) {
        if (entity.getAssignedAt() == null) {
            return null;
        }

        // If already discharged, use discharge time
        Instant endTime = entity.getDischargedAt() != null
                ? entity.getDischargedAt()
                : Instant.now();

        long seconds = endTime.getEpochSecond() - entity.getAssignedAt().getEpochSecond();
        return (int) Math.round(seconds / 60.0);
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
