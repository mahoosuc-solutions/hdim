package com.healthdata.clinicalworkflow.api.v1.mapper;

import com.healthdata.clinicalworkflow.api.v1.dto.QueuePositionResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.QueueStatusResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.QueueWaitTimeResponse;
import com.healthdata.clinicalworkflow.domain.model.WaitingQueueEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper for converting WaitingQueueEntity to response DTOs.
 *
 * Handles type conversions:
 * - UUID → String (for patientId)
 * - Instant → LocalDateTime (for timestamps)
 * - Queue statistics calculation (average wait, longest wait, counts)
 * - Priority level mapping (entity → API format)
 *
 * HIPAA Compliance: No PHI caching - stateless mapper only
 * Multi-tenant: Preserves tenant_id for response filtering
 */
@Component
public class WaitingQueueMapper {

    /**
     * Convert WaitingQueueEntity to QueuePositionResponse DTO
     *
     * @param entity WaitingQueueEntity from database
     * @return QueuePositionResponse for API response
     */
    public QueuePositionResponse toQueuePositionResponse(WaitingQueueEntity entity) {
        if (entity == null) {
            return null;
        }

        Integer actualWaitMinutes = calculateActualWaitTime(entity);
        Integer patientsAhead = entity.getQueuePosition() != null ? entity.getQueuePosition() - 1 : 0;
        String apiStatus = mapQueueStatus(entity.getStatus());
        String apiPriority = mapPriority(entity.getPriority());

        return QueuePositionResponse.builder()
                .id(entity.getId())
                .patientId(uuidToString(entity.getPatientId()))
                .patientName(null)  // Populated by service layer if needed
                .encounterId(null)  // Can be derived from appointment if needed
                .queueType("PROVIDER")  // Default - can be enhanced with actual queue type
                .status(apiStatus)
                .priority(apiPriority)
                .position(entity.getQueuePosition())
                .patientsAhead(patientsAhead)
                .estimatedWaitMinutes(entity.getEstimatedWaitMinutes())
                .actualWaitMinutes(actualWaitMinutes)
                .enteredQueueAt(instantToLocalDateTime(entity.getEnteredQueueAt()))
                .calledAt(instantToLocalDateTime(entity.getCalledAt()))
                .visitType(null)  // Can be populated from appointment if needed
                .providerId(entity.getProviderAssigned())
                .providerName(null)  // Populated by service layer if needed
                .notes(entity.getNotes())
                .tenantId(entity.getTenantId())
                .build();
    }

    /**
     * Convert list of WaitingQueueEntity and calculate statistics for QueueStatusResponse
     *
     * @param entities List of active queue entries
     * @return QueueStatusResponse with queue statistics
     */
    public QueueStatusResponse toQueueStatusResponse(List<WaitingQueueEntity> entities) {
        if (entities == null) {
            entities = List.of();
        }

        List<QueuePositionResponse> queueEntries = entities.stream()
                .map(this::toQueuePositionResponse)
                .toList();

        // Calculate statistics
        int totalPatients = queueEntries.size();

        // Calculate wait time statistics
        List<Integer> waitTimes = entities.stream()
                .map(this::calculateActualWaitTime)
                .filter(Objects::nonNull)
                .toList();

        Integer averageWait = waitTimes.isEmpty()
                ? 0
                : (int) waitTimes.stream().mapToInt(Integer::intValue).average().orElse(0);

        Integer longestWait = waitTimes.isEmpty()
                ? 0
                : waitTimes.stream().mapToInt(Integer::intValue).max().orElse(0);

        // Calculate counts by priority
        Map<String, Integer> countsByPriority = entities.stream()
                .map(e -> mapPriority(e.getPriority()))
                .collect(Collectors.groupingBy(
                        priority -> priority,
                        Collectors.summingInt(e -> 1)
                ));

        // Placeholder queue type counts (can be enhanced with actual queue type tracking)
        int checkInCount = 0;
        int vitalsCount = 0;
        int providerCount = totalPatients;  // Default all to provider queue
        int checkoutCount = 0;

        return QueueStatusResponse.builder()
                .queueEntries(queueEntries)
                .totalPatients(totalPatients)
                .checkInQueueCount(checkInCount)
                .vitalsQueueCount(vitalsCount)
                .providerQueueCount(providerCount)
                .checkoutQueueCount(checkoutCount)
                .averageWaitMinutes(averageWait)
                .longestWaitMinutes(longestWait)
                .countsByPriority(countsByPriority)
                .countsByVisitType(new HashMap<>())  // Placeholder - can be enhanced
                .build();
    }

    /**
     * Create QueueWaitTimeResponse from calculated wait time statistics
     *
     * Note: This method takes pre-calculated wait time values as the entity
     * doesn't contain per-queue-type wait times. Service layer should calculate
     * these based on queue type and current queue load.
     *
     * @param checkInWait Estimated wait for check-in queue
     * @param vitalsWait Estimated wait for vitals queue
     * @param providerWait Estimated wait for provider queue
     * @param checkoutWait Estimated wait for checkout queue
     * @return QueueWaitTimeResponse with wait time estimates
     */
    public QueueWaitTimeResponse toQueueWaitTimeResponse(
            Integer checkInWait,
            Integer vitalsWait,
            Integer providerWait,
            Integer checkoutWait) {

        checkInWait = checkInWait != null ? checkInWait : 0;
        vitalsWait = vitalsWait != null ? vitalsWait : 0;
        providerWait = providerWait != null ? providerWait : 0;
        checkoutWait = checkoutWait != null ? checkoutWait : 0;

        int total = checkInWait + vitalsWait + providerWait + checkoutWait;
        int average = total > 0 ? total / 4 : 0;

        return QueueWaitTimeResponse.builder()
                .checkInWaitMinutes(checkInWait)
                .vitalsWaitMinutes(vitalsWait)
                .providerWaitMinutes(providerWait)
                .checkoutWaitMinutes(checkoutWait)
                .totalEstimatedMinutes(total)
                .averageWaitMinutes(average)
                .build();
    }

    /**
     * Alternative method: Create QueueWaitTimeResponse from list of entities
     * Calculates average wait times from current queue state
     */
    public QueueWaitTimeResponse toQueueWaitTimeResponse(List<WaitingQueueEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return toQueueWaitTimeResponse(0, 0, 0, 0);
        }

        List<Integer> waitTimes = entities.stream()
                .map(this::calculateActualWaitTime)
                .filter(Objects::nonNull)
                .toList();

        Integer averageWait = waitTimes.isEmpty()
                ? 0
                : (int) waitTimes.stream().mapToInt(Integer::intValue).average().orElse(0);

        // Use average wait as estimate for all queue types (simplified)
        // Service layer should provide more accurate per-queue-type estimates
        return toQueueWaitTimeResponse(averageWait, averageWait, averageWait, averageWait);
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Map entity queue status to API status enum
     * Entity: waiting, called, in-room, completed, cancelled
     * API: WAITING, CALLED, IN_PROGRESS, COMPLETED
     */
    private String mapQueueStatus(String entityStatus) {
        if (entityStatus == null) {
            return "WAITING";
        }

        return switch (entityStatus.toLowerCase()) {
            case "waiting" -> "WAITING";
            case "called" -> "CALLED";
            case "in-room" -> "IN_PROGRESS";
            case "completed" -> "COMPLETED";
            case "cancelled" -> "COMPLETED";  // Cancelled treated as completed
            default -> "WAITING";
        };
    }

    /**
     * Map entity priority to API priority enum
     * Entity: urgent, high, normal, low
     * API: STAT, URGENT, ROUTINE
     */
    private String mapPriority(String entityPriority) {
        if (entityPriority == null) {
            return "ROUTINE";
        }

        return switch (entityPriority.toLowerCase()) {
            case "urgent" -> "STAT";
            case "high" -> "URGENT";
            case "normal", "low" -> "ROUTINE";
            default -> "ROUTINE";
        };
    }

    /**
     * Calculate actual wait time in minutes
     * If still waiting: current time - entered queue time
     * If already called/exited: exit time - entered queue time
     */
    private Integer calculateActualWaitTime(WaitingQueueEntity entity) {
        if (entity.getEnteredQueueAt() == null) {
            return null;
        }

        Instant endTime = entity.getExitedQueueAt() != null
                ? entity.getExitedQueueAt()
                : Instant.now();

        long seconds = endTime.getEpochSecond() - entity.getEnteredQueueAt().getEpochSecond();
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
