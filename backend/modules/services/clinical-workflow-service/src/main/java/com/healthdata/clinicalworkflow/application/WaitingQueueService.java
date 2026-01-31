package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.api.v1.dto.QueuePositionResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.QueueStatusResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.QueueWaitTimeResponse;
import com.healthdata.clinicalworkflow.api.v1.dto.QueueEntryRequest;
import com.healthdata.clinicalworkflow.domain.model.WaitingQueueEntity;
import com.healthdata.clinicalworkflow.domain.repository.WaitingQueueRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Waiting Queue Service
 *
 * Manages waiting room queue with priority-based triage for Medical Assistants:
 * - Add patients to queue
 * - Priority-based queue management (urgent, high, normal, low)
 * - Call patients from queue
 * - Wait time estimation
 * - Real-time queue status
 *
 * HIPAA Compliance:
 * - All methods enforce multi-tenant isolation
 * - Audit logging via @Audited annotations (to be added at controller level)
 * - Cache TTL <= 5 minutes for PHI data
 *
 * Integration Points:
 * - WebSocket: Real-time queue updates
 * - RoomManagementService: Triggered when patient called
 * - Analytics: Wait time metrics for quality reporting
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WaitingQueueService {

    private final WaitingQueueRepository queueRepository;

    /**
     * 4b. Fix addToQueue adapter (Line 126)
     * Add patient to queue with DTO request processing
     *
     * Adapter method that extracts fields from QueueEntryRequest and converts parameter types.
     *
     * @param tenantId the tenant ID
     * @param request the queue entry request with patient and priority info
     * @param userId the user ID creating the queue entry
     * @return queue position response
     */
    @Transactional
    @CacheEvict(value = {"waitingQueue", "queueStatus"}, key = "#tenantId", allEntries = true)
    public QueuePositionResponse addToQueue(
            String tenantId,
            QueueEntryRequest request,
            String userId) {
        log.debug("Adding patient {} to queue in tenant {} by user {}",
                request.getPatientId(), tenantId, userId);

        UUID patientId;
        try {
            patientId = UUID.fromString(request.getPatientId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid patient ID format: " + request.getPatientId());
        }

        // Extract priority from request, default to ROUTINE
        String priority = request.getPriority() != null ?
                request.getPriority().toLowerCase() : "routine";

        WaitingQueueEntity saved = addToQueueWithPriority(
                patientId,
                request.getEncounterId(),
                priority,
                tenantId);

        return mapToQueuePositionResponse(saved, request);
    }

    /**
     * Add patient to queue (original implementation)
     *
     * Main workflow for adding patient to waiting queue after check-in.
     *
     * @param patientId the patient ID
     * @param appointmentId the appointment ID (optional)
     * @param tenantId the tenant ID
     * @return created queue entry
     */
    @Transactional
    @CacheEvict(value = {"waitingQueue", "queueStatus"}, key = "#tenantId", allEntries = true)
    public WaitingQueueEntity addToQueueInternal(UUID patientId, String appointmentId, String tenantId) {
        return addToQueueWithPriority(patientId, appointmentId, "normal", tenantId);
    }

    /**
     * Add patient to queue with specific priority
     *
     * @param patientId the patient ID
     * @param appointmentId the appointment ID (optional)
     * @param priority the priority level
     * @param tenantId the tenant ID
     * @return created queue entry
     */
    @Transactional
    @CacheEvict(value = {"waitingQueue", "queueStatus"}, key = "#tenantId", allEntries = true)
    public WaitingQueueEntity addToQueueWithPriority(
            UUID patientId, String appointmentId, String priority, String tenantId) {
        log.debug("Adding patient {} to queue with priority {} in tenant {}",
                patientId, priority, tenantId);

        // Check if patient already in queue
        queueRepository.findPatientQueuePosition(patientId, tenantId)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Patient already in queue at position: " + existing.getQueuePosition());
                });

        // Determine estimated wait time based on priority
        Integer estimatedWait = calculateEstimatedWait(tenantId, priority);

        WaitingQueueEntity queueEntry = WaitingQueueEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .appointmentId(appointmentId)
                .queuePosition(0) // Will be updated
                .priority(priority)
                .status("waiting")
                .enteredQueueAt(Instant.now())
                .estimatedWaitMinutes(estimatedWait)
                .build();

        WaitingQueueEntity saved = queueRepository.save(queueEntry);

        // Update queue positions for all waiting patients
        prioritizeQueue(tenantId);

        log.info("Patient {} added to queue at priority {} in tenant {}",
                patientId, priority, tenantId);

        return saved;
    }

    /**
     * Prioritize queue
     *
     * Reorders queue based on priority levels and updates positions.
     * Called after adding patients or changing priorities.
     *
     * @param tenantId the tenant ID
     */
    @Transactional
    @CacheEvict(value = {"waitingQueue", "queueStatus"}, key = "#tenantId", allEntries = true)
    public void prioritizeQueue(String tenantId) {
        log.debug("Prioritizing queue for tenant {}", tenantId);

        List<WaitingQueueEntity> waiting = queueRepository.findWaitingPatientsByTenant(tenantId);

        // Update positions based on priority order
        int position = 1;
        for (WaitingQueueEntity entry : waiting) {
            entry.setQueuePosition(position++);
            queueRepository.save(entry);
        }

        log.info("Queue prioritized for tenant {} ({} patients)", tenantId, waiting.size());
    }

    /**
     * Call patient from queue
     *
     * Updates patient status to called and records call time.
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return updated queue entry
     */
    @Transactional
    @CacheEvict(value = {"waitingQueue", "queueStatus"}, key = "#tenantId", allEntries = true)
    public WaitingQueueEntity callPatient(UUID patientId, String tenantId) {
        log.debug("Calling patient {} in tenant {}", patientId, tenantId);

        WaitingQueueEntity queueEntry = queueRepository
                .findPatientQueuePosition(patientId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not in queue: " + patientId));

        queueEntry.setStatus("called");
        queueEntry.setCalledAt(Instant.now());

        WaitingQueueEntity updated = queueRepository.save(queueEntry);

        log.info("Patient {} called from queue in tenant {}", patientId, tenantId);

        return updated;
    }

    /**
     * Remove patient from queue
     *
     * Removes patient from queue and updates positions.
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     */
    @Transactional
    @CacheEvict(value = {"waitingQueue", "queueStatus"}, key = "#tenantId", allEntries = true)
    public void removeFromQueue(UUID patientId, String tenantId) {
        log.debug("Removing patient {} from queue in tenant {}", patientId, tenantId);

        WaitingQueueEntity queueEntry = queueRepository
                .findPatientQueuePosition(patientId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not in queue: " + patientId));

        queueEntry.setStatus("completed");
        queueEntry.setExitedQueueAt(Instant.now());

        queueRepository.save(queueEntry);

        // Update queue positions
        prioritizeQueue(tenantId);

        log.info("Patient {} removed from queue in tenant {}", patientId, tenantId);
    }

    /**
     * Calculate estimated wait time
     *
     * Estimates wait time based on historical data for priority level.
     *
     * @param tenantId the tenant ID
     * @param priority the priority level
     * @return estimated wait time in minutes
     */
    public Integer calculateEstimatedWait(String tenantId, String priority) {
        log.debug("Calculating estimated wait for priority {} in tenant {}", priority, tenantId);

        Integer avgWait = queueRepository.getEstimatedWaitTime(tenantId, priority);

        // If no historical data, use defaults based on priority
        if (avgWait == null) {
            return switch (priority.toLowerCase()) {
                case "urgent" -> 5;
                case "high" -> 15;
                case "normal" -> 30;
                case "low" -> 45;
                default -> 30;
            };
        }

        return avgWait;
    }

    /**
     * 4a. Fix getQueueStatus return type (Line 72)
     * Get queue status with DTO response mapping
     *
     * Retrieves summary of queue status for dashboard, mapped to response DTO.
     *
     * @param tenantId the tenant ID
     * @return queue status response
     */
    @Cacheable(value = "queueStatus", key = "#tenantId")
    public QueueStatusResponse getQueueStatus(String tenantId) {
        log.debug("Retrieving queue status for tenant {}", tenantId);

        List<WaitingQueueEntity> allQueued = getWaitingPatients(tenantId);
        long totalWaiting = allQueued.size();

        int checkInCount = (int) allQueued.stream()
                .filter(e -> "CHECK_IN".equals(e.getAppointmentId()))
                .count();

        int vitalsCount = (int) allQueued.stream()
                .filter(e -> "VITALS".equals(e.getAppointmentId()))
                .count();

        int providerCount = (int) allQueued.stream()
                .filter(e -> "PROVIDER".equals(e.getAppointmentId()))
                .count();

        int checkoutCount = (int) allQueued.stream()
                .filter(e -> "CHECKOUT".equals(e.getAppointmentId()))
                .count();

        int averageWait = allQueued.isEmpty() ? 0 :
                (int) allQueued.stream()
                    .mapToInt(e -> e.getEstimatedWaitMinutes() != null ? e.getEstimatedWaitMinutes() : 0)
                    .average()
                    .orElse(0);

        int longestWait = allQueued.stream()
                .mapToInt(e -> e.getEstimatedWaitMinutes() != null ? e.getEstimatedWaitMinutes() : 0)
                .max()
                .orElse(0);

        // Group by priority
        Map<String, Integer> countsByPriority = allQueued.stream()
                .collect(Collectors.groupingBy(
                        WaitingQueueEntity::getPriority,
                        Collectors.collectingAndThen(Collectors.toList(), List::size)));

        return QueueStatusResponse.builder()
                .queueEntries(mapToQueuePositionResponseList(allQueued))
                .totalPatients((int) totalWaiting)
                .checkInQueueCount(checkInCount)
                .vitalsQueueCount(vitalsCount)
                .providerQueueCount(providerCount)
                .checkoutQueueCount(checkoutCount)
                .averageWaitMinutes(averageWait)
                .longestWaitMinutes(longestWait)
                .countsByPriority(countsByPriority)
                .build();
    }

    /**
     * Get queue status (internal - returns inner QueueStatus class)
     *
     * @param tenantId the tenant ID
     * @return queue status summary
     */
    @Cacheable(value = "queueStatus", key = "#tenantId")
    public QueueStatus getQueueStatusInternal(String tenantId) {
        log.debug("Retrieving queue status for tenant {}", tenantId);

        long waitingCount = queueRepository.countWaitingPatients(tenantId);
        List<WaitingQueueEntity> urgent = queueRepository.findUrgentPatients(tenantId);

        return QueueStatus.builder()
                .totalWaiting(waitingCount)
                .urgentCount(urgent.size())
                .averageWaitMinutes(queueRepository.getEstimatedWaitTime(tenantId, "normal"))
                .build();
    }

    /**
     * Get waiting patients
     *
     * @param tenantId the tenant ID
     * @return list of waiting patients
     */
    @Cacheable(value = "waitingQueue", key = "#tenantId")
    public List<WaitingQueueEntity> getWaitingPatients(String tenantId) {
        log.debug("Retrieving waiting patients for tenant {}", tenantId);

        return queueRepository.findWaitingPatientsByTenant(tenantId);
    }

    /**
     * Update queue positions (internal use)
     *
     * Recalculates queue positions after changes.
     *
     * @param tenantId the tenant ID
     */
    @Transactional
    public void updateQueuePosition(String tenantId) {
        prioritizeQueue(tenantId);
    }

    /**
     * Get patient queue info
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return queue entry for patient
     */
    public WaitingQueueEntity getPatientQueueInfo(UUID patientId, String tenantId) {
        log.debug("Retrieving queue info for patient {} in tenant {}", patientId, tenantId);

        return queueRepository.findPatientQueuePosition(patientId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not in queue: " + patientId));
    }

    /**
     * Get queue by ID
     *
     * @param queueId the queue entry ID
     * @param tenantId the tenant ID
     * @return queue entry
     */
    public WaitingQueueEntity getQueueById(UUID queueId, String tenantId) {
        log.debug("Retrieving queue entry {} in tenant {}", queueId, tenantId);

        return queueRepository.findByIdAndTenantId(queueId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Queue entry not found: " + queueId));
    }

    /**
     * 4c. Fix getPatientQueueInfo parameter types (Line 158)
     * Get patient's queue information with String patientId parameter
     *
     * Adapter that converts String patientId to UUID for lookup.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID as String (converted to UUID)
     * @return queue position response
     */
    public QueuePositionResponse getPatientQueueInfo(String tenantId, String patientId) {
        log.debug("Retrieving queue info for patient {} in tenant {}", patientId, tenantId);

        UUID pid;
        try {
            pid = UUID.fromString(patientId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid patient ID format: " + patientId);
        }

        WaitingQueueEntity queueEntry = queueRepository
                .findPatientQueuePosition(pid, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not in queue: " + patientId));

        return mapToQueuePositionResponse(queueEntry, null);
    }

    /**
     * Get patient queue info (internal - returns entity)
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return queue entry for patient
     */
    public WaitingQueueEntity getPatientQueueInfoInternal(UUID patientId, String tenantId) {
        log.debug("Retrieving queue info for patient {} in tenant {}", patientId, tenantId);

        return queueRepository.findPatientQueuePosition(patientId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not in queue: " + patientId));
    }

    /**
     * 4d. Fix callPatient signature (Line 192)
     * Call patient from queue with String patientId parameter
     *
     * Updates patient status to called and records call time.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID as String
     * @param userId the user ID calling the patient
     * @return queue position response
     */
    @Transactional
    @CacheEvict(value = {"waitingQueue", "queueStatus"}, key = "#tenantId", allEntries = true)
    public QueuePositionResponse callPatient(String tenantId, String patientId, String userId) {
        log.debug("Calling patient {} in tenant {} by user {}", patientId, tenantId, userId);

        UUID pid;
        try {
            pid = UUID.fromString(patientId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid patient ID format: " + patientId);
        }

        WaitingQueueEntity updated = callPatientInternal(pid, tenantId);
        return mapToQueuePositionResponse(updated, null);
    }

    /**
     * Call patient (internal - returns entity)
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return updated queue entry
     */
    @Transactional
    @CacheEvict(value = {"waitingQueue", "queueStatus"}, key = "#tenantId", allEntries = true)
    public WaitingQueueEntity callPatientInternal(UUID patientId, String tenantId) {
        log.debug("Calling patient {} in tenant {}", patientId, tenantId);

        WaitingQueueEntity queueEntry = queueRepository
                .findPatientQueuePosition(patientId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not in queue: " + patientId));

        queueEntry.setStatus("called");
        queueEntry.setCalledAt(Instant.now());

        WaitingQueueEntity updated = queueRepository.save(queueEntry);

        log.info("Patient {} called from queue in tenant {}", patientId, tenantId);

        return updated;
    }

    /**
     * 4e. Fix removeFromQueue signature (Line 225)
     * Remove patient from queue with String patientId parameter
     *
     * Removes patient from queue and updates positions.
     *
     * @param tenantId the tenant ID
     * @param patientId the patient ID as String
     * @param userId the user ID removing from queue
     */
    @Transactional
    @CacheEvict(value = {"waitingQueue", "queueStatus"}, key = "#tenantId", allEntries = true)
    public void removeFromQueue(String tenantId, String patientId, String userId) {
        log.debug("Removing patient {} from queue in tenant {} by user {}", patientId, tenantId, userId);

        UUID pid;
        try {
            pid = UUID.fromString(patientId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid patient ID format: " + patientId);
        }

        removeFromQueueInternal(pid, tenantId);
    }

    /**
     * Remove patient from queue (internal)
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     */
    @Transactional
    @CacheEvict(value = {"waitingQueue", "queueStatus"}, key = "#tenantId", allEntries = true)
    public void removeFromQueueInternal(UUID patientId, String tenantId) {
        log.debug("Removing patient {} from queue in tenant {}", patientId, tenantId);

        WaitingQueueEntity queueEntry = queueRepository
                .findPatientQueuePosition(patientId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not in queue: " + patientId));

        queueEntry.setStatus("completed");
        queueEntry.setExitedQueueAt(Instant.now());

        queueRepository.save(queueEntry);

        // Update queue positions
        prioritizeQueue(tenantId);

        log.info("Patient {} removed from queue in tenant {}", patientId, tenantId);
    }

    /**
     * 4f. Add getWaitTimes method (Line 250)
     * Get estimated wait times by queue type
     *
     * Calculate average wait times by priority level.
     *
     * @param tenantId the tenant ID
     * @return wait time response with estimates
     */
    public QueueWaitTimeResponse getWaitTimes(String tenantId) {
        log.debug("Calculating wait times for tenant {}", tenantId);

        Integer checkInWait = calculateEstimatedWaitByAppointmentType(tenantId, "CHECK_IN");
        Integer vitalsWait = calculateEstimatedWaitByAppointmentType(tenantId, "VITALS");
        Integer providerWait = calculateEstimatedWaitByAppointmentType(tenantId, "PROVIDER");
        Integer checkoutWait = calculateEstimatedWaitByAppointmentType(tenantId, "CHECKOUT");

        Integer totalEstimated = (checkInWait != null ? checkInWait : 0) +
                                (vitalsWait != null ? vitalsWait : 0) +
                                (providerWait != null ? providerWait : 0) +
                                (checkoutWait != null ? checkoutWait : 0);

        Integer averageWait = totalEstimated > 0 ? totalEstimated / 4 : 0;

        return QueueWaitTimeResponse.builder()
                .checkInWaitMinutes(checkInWait)
                .vitalsWaitMinutes(vitalsWait)
                .providerWaitMinutes(providerWait)
                .checkoutWaitMinutes(checkoutWait)
                .totalEstimatedMinutes(totalEstimated)
                .averageWaitMinutes(averageWait)
                .build();
    }

    /**
     * Calculate estimated wait time by appointment type
     *
     * @param tenantId the tenant ID
     * @param appointmentType the appointment type
     * @return estimated wait in minutes
     */
    private Integer calculateEstimatedWaitByAppointmentType(String tenantId, String appointmentType) {
        List<WaitingQueueEntity> queued = getWaitingPatients(tenantId);
        List<WaitingQueueEntity> typeQueued = queued.stream()
                .filter(e -> appointmentType.equals(e.getAppointmentId()))
                .toList();

        if (typeQueued.isEmpty()) {
            return 0;
        }

        return (int) typeQueued.stream()
                .mapToInt(e -> e.getEstimatedWaitMinutes() != null ? e.getEstimatedWaitMinutes() : 0)
                .average()
                .orElse(0);
    }

    /**
     * 4g. Fix getQueueByPriority return type (Line 276)
     * Get queue entries grouped by priority
     *
     * Returns Map of priority level to list of queue position responses.
     *
     * @param tenantId the tenant ID
     * @param queueType optional queue type filter (not used in current impl)
     * @return map of priority to queue position responses
     */
    public Map<String, List<QueuePositionResponse>> getQueueByPriority(
            String tenantId,
            String queueType) {
        log.debug("Retrieving queue by priority for tenant {}", tenantId);

        // Get all waiting patients and group by priority
        List<WaitingQueueEntity> allEntries = getWaitingPatients(tenantId);

        return allEntries.stream()
                .collect(Collectors.groupingBy(
                        WaitingQueueEntity::getPriority,
                        Collectors.mapping(
                                entity -> mapToQueuePositionResponse(entity, null),
                                Collectors.toList())));
    }

    /**
     * Get queue by priority (internal - returns entity map)
     *
     * @param priority the priority level
     * @param tenantId the tenant ID
     * @return list of queue entries
     */
    public List<WaitingQueueEntity> getQueueByPriorityInternal(String priority, String tenantId) {
        log.debug("Retrieving queue for priority {} in tenant {}", priority, tenantId);

        return queueRepository.findQueueByPriority(tenantId, priority);
    }

    /**
     * 4h. Add reorderQueue method (Line 303)
     * Reorder queue by priority and return updated status
     *
     * Recalculates queue positions based on priority and wait time.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID requesting reorder
     * @return updated queue status response
     */
    @Transactional
    @CacheEvict(value = {"waitingQueue", "queueStatus"}, key = "#tenantId", allEntries = true)
    public QueueStatusResponse reorderQueue(String tenantId, String userId) {
        log.debug("Reordering queue for tenant {} by user {}", tenantId, userId);

        prioritizeQueue(tenantId);
        return getQueueStatus(tenantId);
    }

    /**
     * Get patient queue history
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return list of all queue entries for patient
     */
    public List<WaitingQueueEntity> getPatientQueueHistory(UUID patientId, String tenantId) {
        log.debug("Retrieving queue history for patient {} in tenant {}", patientId, tenantId);

        return queueRepository.findByTenantIdAndPatientIdOrderByEnteredQueueAtDesc(
                tenantId, patientId);
    }

    /**
     * Get next patient in queue
     *
     * @param tenantId the tenant ID
     * @return next patient to be called
     */
    public WaitingQueueEntity getNextPatient(String tenantId) {
        log.debug("Retrieving next patient in queue for tenant {}", tenantId);

        return queueRepository.findNextPatientInQueue(tenantId)
                .orElse(null);
    }

    /**
     * Map WaitingQueueEntity to QueuePositionResponse
     *
     * @param entity the queue entity
     * @param request the original request (for additional fields, can be null)
     * @return mapped response
     */
    private QueuePositionResponse mapToQueuePositionResponse(WaitingQueueEntity entity, QueueEntryRequest request) {
        if (entity == null) {
            return null;
        }

        LocalDateTime enteredAt = entity.getEnteredQueueAt() != null ?
                LocalDateTime.ofInstant(entity.getEnteredQueueAt(), ZoneId.systemDefault()) : null;

        LocalDateTime calledAtTime = entity.getCalledAt() != null ?
                LocalDateTime.ofInstant(entity.getCalledAt(), ZoneId.systemDefault()) : null;

        return QueuePositionResponse.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId().toString())
                .encounterId(entity.getAppointmentId())
                .queueType(request != null ? request.getQueueType() : entity.getAppointmentId())
                .status(entity.getStatus() != null ? entity.getStatus().toUpperCase() : "UNKNOWN")
                .priority(entity.getPriority() != null ? entity.getPriority().toUpperCase() : "ROUTINE")
                .position(entity.getQueuePosition())
                .patientsAhead(Math.max(0, entity.getQueuePosition() - 1))
                .estimatedWaitMinutes(entity.getEstimatedWaitMinutes())
                .actualWaitMinutes(entity.getCurrentWaitTimeMinutes())
                .enteredQueueAt(enteredAt)
                .calledAt(calledAtTime)
                .visitType(request != null ? request.getVisitType() : null)
                .providerId(entity.getProviderAssigned())
                .notes(entity.getNotes())
                .tenantId(entity.getTenantId())
                .build();
    }

    /**
     * Map list of WaitingQueueEntity to list of QueuePositionResponse
     *
     * @param entities the queue entities
     * @return mapped response list
     */
    private List<QueuePositionResponse> mapToQueuePositionResponseList(List<WaitingQueueEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }

        return entities.stream()
                .map(entity -> mapToQueuePositionResponse(entity, null))
                .toList();
    }

    /**
     * Queue Status DTO (inner class)
     * Represents internal queue status structure
     */
    @Data
    @Builder
    public static class QueueStatus {
        private long totalWaiting;
        private long urgentCount;
        private Integer averageWaitMinutes;
    }
}
