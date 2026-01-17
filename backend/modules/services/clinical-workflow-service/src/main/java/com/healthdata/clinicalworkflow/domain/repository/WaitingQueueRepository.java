package com.healthdata.clinicalworkflow.domain.repository;

import com.healthdata.clinicalworkflow.domain.model.WaitingQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WaitingQueueEntity
 *
 * Provides data access for waiting room queue management with multi-tenant isolation.
 * Includes priority-based queue operations and wait time calculations.
 *
 * HIPAA Compliance:
 * - All PHI access is audited
 * - Cache TTL must be <= 5 minutes
 * - Multi-tenant filtering enforced on all queries
 */
@Repository
@Transactional(readOnly = true)
public interface WaitingQueueRepository extends JpaRepository<WaitingQueueEntity, UUID> {

    /**
     * Find waiting queue entry by ID and tenant
     *
     * @param id UUID of the queue entry
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the queue entry if found
     */
    Optional<WaitingQueueEntity> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find all waiting patients by tenant
     * Returns patients currently in waiting status, ordered by priority and queue position
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return List of queue entries for patients currently waiting
     */
    @Query("""
        SELECT w FROM WaitingQueueEntity w
        WHERE w.tenantId = :tenantId
        AND w.status = 'waiting'
        ORDER BY w.priority ASC, w.queuePosition ASC
    """)
    List<WaitingQueueEntity> findWaitingPatientsByTenant(
        @Param("tenantId") String tenantId
    );

    /**
     * Find queue entries by priority level and tenant
     * Used for priority-based queue management
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param priority Priority level (urgent, high, normal, low)
     * @return List of queue entries with the specified priority
     */
    @Query("""
        SELECT w FROM WaitingQueueEntity w
        WHERE w.tenantId = :tenantId
        AND w.priority = :priority
        AND w.status = 'waiting'
        ORDER BY w.queuePosition ASC
    """)
    List<WaitingQueueEntity> findQueueByPriority(
        @Param("tenantId") String tenantId,
        @Param("priority") String priority
    );

    /**
     * Find patient's current queue position
     * Returns the active queue entry for a specific patient
     *
     * @param patientId UUID of the patient
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the patient's queue entry if found
     */
    @Query("""
        SELECT w FROM WaitingQueueEntity w
        WHERE w.tenantId = :tenantId
        AND w.patientId = :patientId
        AND w.status IN ('waiting', 'called')
        ORDER BY w.enteredQueueAt DESC
        LIMIT 1
    """)
    Optional<WaitingQueueEntity> findPatientQueuePosition(
        @Param("patientId") UUID patientId,
        @Param("tenantId") String tenantId
    );

    /**
     * Calculate estimated wait time for a priority level
     * Returns the average wait time in minutes for patients with the specified priority
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param priority Priority level (urgent, high, normal, low)
     * @return Average wait time in minutes, or null if no data
     */
    @Query("""
        SELECT AVG(w.waitTimeMinutes) FROM WaitingQueueEntity w
        WHERE w.tenantId = :tenantId
        AND w.priority = :priority
        AND w.waitTimeMinutes IS NOT NULL
        AND w.exitedQueueAt IS NOT NULL
    """)
    Integer getEstimatedWaitTime(
        @Param("tenantId") String tenantId,
        @Param("priority") String priority
    );

    /**
     * Find queue entries by status and tenant
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param status Queue status (waiting, called, in-room, completed, cancelled)
     * @return List of queue entries with the specified status
     */
    List<WaitingQueueEntity> findByTenantIdAndStatusOrderByEnteredQueueAtAsc(
        String tenantId,
        String status
    );

    /**
     * Find all queue entries for a patient
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param patientId UUID of the patient
     * @return List of all queue entries for the patient
     */
    List<WaitingQueueEntity> findByTenantIdAndPatientIdOrderByEnteredQueueAtDesc(
        String tenantId,
        UUID patientId
    );

    /**
     * Count patients currently waiting by tenant
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Count of patients with waiting status
     */
    @Query("""
        SELECT COUNT(w) FROM WaitingQueueEntity w
        WHERE w.tenantId = :tenantId
        AND w.status = 'waiting'
    """)
    long countWaitingPatients(@Param("tenantId") String tenantId);

    /**
     * Find next patient to be called (highest priority, lowest queue position)
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the next patient in queue
     */
    @Query("""
        SELECT w FROM WaitingQueueEntity w
        WHERE w.tenantId = :tenantId
        AND w.status = 'waiting'
        ORDER BY w.priority ASC, w.queuePosition ASC
        LIMIT 1
    """)
    Optional<WaitingQueueEntity> findNextPatientInQueue(@Param("tenantId") String tenantId);

    /**
     * Find urgent patients in queue
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return List of urgent priority queue entries
     */
    @Query("""
        SELECT w FROM WaitingQueueEntity w
        WHERE w.tenantId = :tenantId
        AND w.priority = 'urgent'
        AND w.status = 'waiting'
        ORDER BY w.queuePosition ASC
    """)
    List<WaitingQueueEntity> findUrgentPatients(@Param("tenantId") String tenantId);
}
