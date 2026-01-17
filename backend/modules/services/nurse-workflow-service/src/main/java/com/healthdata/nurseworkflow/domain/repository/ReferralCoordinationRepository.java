package com.healthdata.nurseworkflow.domain.repository;

import com.healthdata.nurseworkflow.domain.model.ReferralCoordinationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ReferralCoordinationEntity
 *
 * Provides data access for referral tracking with multi-tenant isolation.
 */
@Repository
public interface ReferralCoordinationRepository extends JpaRepository<ReferralCoordinationEntity, UUID> {

    /**
     * Find referrals for a specific patient
     */
    Page<ReferralCoordinationEntity> findByTenantIdAndPatientIdOrderByRequestedAtDesc(
        String tenantId,
        UUID patientId,
        Pageable pageable
    );

    /**
     * Find pending referrals (not yet completed/closed)
     */
    @Query("""
        SELECT r FROM ReferralCoordinationEntity r
        WHERE r.tenantId = :tenantId
        AND r.status IN ('PENDING_AUTHORIZATION', 'AUTHORIZED', 'SCHEDULED', 'AWAITING_APPOINTMENT')
        ORDER BY r.requestedAt ASC
    """)
    Page<ReferralCoordinationEntity> findPendingByTenant(
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    /**
     * Find referrals by status
     */
    Page<ReferralCoordinationEntity> findByTenantIdAndStatusOrderByRequestedAtDesc(
        String tenantId,
        ReferralCoordinationEntity.ReferralStatus status,
        Pageable pageable
    );

    /**
     * Find referrals by specialty type
     */
    Page<ReferralCoordinationEntity> findByTenantIdAndSpecialtyTypeOrderByRequestedAtDesc(
        String tenantId,
        String specialtyType,
        Pageable pageable
    );

    /**
     * Find referrals by priority
     */
    Page<ReferralCoordinationEntity> findByTenantIdAndPriorityOrderByRequestedAtDesc(
        String tenantId,
        ReferralCoordinationEntity.ReferralPriority priority,
        Pageable pageable
    );

    /**
     * Find referrals by coordinator (nurse)
     */
    Page<ReferralCoordinationEntity> findByTenantIdAndCoordinatorIdOrderByRequestedAtDesc(
        String tenantId,
        UUID coordinatorId,
        Pageable pageable
    );

    /**
     * Find referrals awaiting appointment scheduling
     */
    @Query("""
        SELECT r FROM ReferralCoordinationEntity r
        WHERE r.tenantId = :tenantId
        AND r.appointmentScheduled = false
        AND r.status IN ('AUTHORIZED', 'AWAITING_APPOINTMENT')
        ORDER BY r.requestedAt ASC
    """)
    List<ReferralCoordinationEntity> findAwaitingAppointmentScheduling(
        @Param("tenantId") String tenantId
    );

    /**
     * Find referrals awaiting follow-up results
     */
    @Query("""
        SELECT r FROM ReferralCoordinationEntity r
        WHERE r.tenantId = :tenantId
        AND r.resultsReceived = false
        AND r.appointmentStatus = 'ATTENDED'
        ORDER BY r.appointmentDate DESC
    """)
    List<ReferralCoordinationEntity> findAwaitingResults(
        @Param("tenantId") String tenantId
    );

    /**
     * Find referrals by FHIR ServiceRequest reference
     */
    Optional<ReferralCoordinationEntity> findByTenantIdAndServiceRequestId(
        String tenantId,
        String serviceRequestId
    );

    /**
     * Find referrals requested within date range
     */
    Page<ReferralCoordinationEntity> findByTenantIdAndRequestedAtBetweenOrderByRequestedAtDesc(
        String tenantId,
        Instant startDate,
        Instant endDate,
        Pageable pageable
    );

    /**
     * Count pending referrals for tenant
     */
    long countByTenantIdAndStatusIn(
        String tenantId,
        java.util.Collection<ReferralCoordinationEntity.ReferralStatus> statuses
    );

    /**
     * Find urgent referrals awaiting appointment
     */
    @Query("""
        SELECT r FROM ReferralCoordinationEntity r
        WHERE r.tenantId = :tenantId
        AND r.priority = 'URGENT'
        AND r.appointmentScheduled = false
        ORDER BY r.requestedAt ASC
    """)
    List<ReferralCoordinationEntity> findUrgentAwaitingScheduling(
        @Param("tenantId") String tenantId
    );
}
