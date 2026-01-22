package com.healthdata.eventstore.repository;

import com.healthdata.eventstore.domain.EventProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * EventProcessingStatusRepository - JPA Repository for event_processing_status table
 *
 * Purpose: Track event processing across multiple consumers
 *
 * Key Queries:
 * - Find consumer status (for resuming processing)
 * - Find failed consumers (for monitoring/alerts)
 * - Find all consumers for a tenant (for tenant management)
 */
@Repository
public interface EventProcessingStatusRepository extends JpaRepository<EventProcessingStatus, Long> {

    /**
     * Find processing status for a specific consumer and tenant
     * Used for resuming event processing after restart
     *
     * @param consumerName Consumer identifier (e.g., "patient-event-handler")
     * @param tenantId     Tenant identifier
     * @return Processing status if exists
     */
    Optional<EventProcessingStatus> findByConsumerNameAndTenantId(
            String consumerName,
            String tenantId
    );

    /**
     * Find all consumers for a tenant
     * Used for tenant-level monitoring and management
     *
     * @param tenantId Tenant identifier
     * @return List of consumer statuses
     */
    List<EventProcessingStatus> findByTenantId(String tenantId);

    /**
     * Find all failed consumers
     * Used for monitoring and alerting
     *
     * @return List of failed consumer statuses
     */
    @Query("""
            SELECT s FROM EventProcessingStatus s
            WHERE s.status = 'FAILED'
            ORDER BY s.lastProcessedAt DESC
            """)
    List<EventProcessingStatus> findFailedConsumers();

    /**
     * Find all failed consumers for a tenant
     * Used for tenant-specific monitoring
     *
     * @param tenantId Tenant identifier
     * @return List of failed consumer statuses
     */
    @Query("""
            SELECT s FROM EventProcessingStatus s
            WHERE s.status = 'FAILED'
              AND s.tenantId = :tenantId
            ORDER BY s.lastProcessedAt DESC
            """)
    List<EventProcessingStatus> findFailedConsumersByTenant(@Param("tenantId") String tenantId);

    /**
     * Find all running consumers
     * Used for monitoring active consumers
     *
     * @return List of running consumer statuses
     */
    @Query("""
            SELECT s FROM EventProcessingStatus s
            WHERE s.status = 'RUNNING'
            ORDER BY s.lastProcessedAt DESC
            """)
    List<EventProcessingStatus> findRunningConsumers();

    /**
     * Find consumers that are lagging behind
     * (Haven't processed events in a while)
     * Used for monitoring and alerting
     *
     * @param lagThresholdSeconds Maximum seconds since last processing before considered lagging
     * @return List of lagging consumer statuses
     */
    @Query(value = """
            SELECT * FROM event_processing_status
            WHERE status = 'RUNNING'
              AND EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - last_processed_at)) > :lagThresholdSeconds
            ORDER BY last_processed_at ASC
            """, nativeQuery = true)
    List<EventProcessingStatus> findLaggingConsumers(@Param("lagThresholdSeconds") long lagThresholdSeconds);

    /**
     * Check if consumer exists for tenant
     * Used for initialization checks
     *
     * @param consumerName Consumer identifier
     * @param tenantId     Tenant identifier
     * @return true if consumer exists
     */
    boolean existsByConsumerNameAndTenantId(String consumerName, String tenantId);

    /**
     * Count active consumers for a tenant
     * Used for monitoring
     *
     * @param tenantId Tenant identifier
     * @return Count of consumers in RUNNING or UP_TO_DATE status
     */
    @Query("""
            SELECT COUNT(s) FROM EventProcessingStatus s
            WHERE s.tenantId = :tenantId
              AND (s.status = 'RUNNING' OR s.status = 'UP_TO_DATE')
            """)
    long countActiveConsumers(@Param("tenantId") String tenantId);
}
