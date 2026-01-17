package com.healthdata.clinicalworkflow.application;

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
import java.util.List;
import java.util.UUID;

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
     * Add patient to queue
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
    public WaitingQueueEntity addToQueue(UUID patientId, String appointmentId, String tenantId) {
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
     * Get queue status
     *
     * Retrieves summary of queue status for dashboard.
     *
     * @param tenantId the tenant ID
     * @return queue status summary
     */
    @Cacheable(value = "queueStatus", key = "#tenantId")
    public QueueStatus getQueueStatus(String tenantId) {
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
     * Get queue by priority
     *
     * @param priority the priority level
     * @param tenantId the tenant ID
     * @return list of queue entries
     */
    public List<WaitingQueueEntity> getQueueByPriority(String priority, String tenantId) {
        log.debug("Retrieving queue for priority {} in tenant {}", priority, tenantId);

        return queueRepository.findQueueByPriority(tenantId, priority);
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
     * Queue Status DTO
     */
    @Data
    @Builder
    public static class QueueStatus {
        private long totalWaiting;
        private long urgentCount;
        private Integer averageWaitMinutes;
    }
}
