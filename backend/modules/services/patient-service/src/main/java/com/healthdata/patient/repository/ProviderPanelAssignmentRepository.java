package com.healthdata.patient.repository;

import com.healthdata.patient.entity.ProviderPanelAssignmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for provider panel assignments.
 * Issue #135: Create Provider Panel Assignment API
 */
@Repository
public interface ProviderPanelAssignmentRepository extends JpaRepository<ProviderPanelAssignmentEntity, UUID> {

    /**
     * Find all active assignments for a provider in a tenant.
     */
    @Query("SELECT p FROM ProviderPanelAssignmentEntity p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.providerId = :providerId " +
           "AND p.active = true " +
           "ORDER BY p.assignedDate DESC")
    Page<ProviderPanelAssignmentEntity> findActiveByTenantAndProvider(
            @Param("tenantId") String tenantId,
            @Param("providerId") UUID providerId,
            Pageable pageable);

    /**
     * Find all active assignments for a provider (unpaged).
     */
    @Query("SELECT p FROM ProviderPanelAssignmentEntity p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.providerId = :providerId " +
           "AND p.active = true")
    List<ProviderPanelAssignmentEntity> findActiveByTenantAndProvider(
            @Param("tenantId") String tenantId,
            @Param("providerId") UUID providerId);

    /**
     * Find assignment for a specific patient under a provider.
     */
    Optional<ProviderPanelAssignmentEntity> findByTenantIdAndProviderIdAndPatientId(
            String tenantId, UUID providerId, UUID patientId);

    /**
     * Find all providers assigned to a patient.
     */
    @Query("SELECT p FROM ProviderPanelAssignmentEntity p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.patientId = :patientId " +
           "AND p.active = true")
    List<ProviderPanelAssignmentEntity> findProvidersByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Count active patients in a provider's panel.
     */
    @Query("SELECT COUNT(p) FROM ProviderPanelAssignmentEntity p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.providerId = :providerId " +
           "AND p.active = true")
    long countActiveByTenantAndProvider(
            @Param("tenantId") String tenantId,
            @Param("providerId") UUID providerId);

    /**
     * Find patient IDs for a provider's panel.
     */
    @Query("SELECT p.patientId FROM ProviderPanelAssignmentEntity p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.providerId = :providerId " +
           "AND p.active = true")
    List<UUID> findPatientIdsByTenantAndProvider(
            @Param("tenantId") String tenantId,
            @Param("providerId") UUID providerId);

    /**
     * Check if a patient is assigned to a provider.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM ProviderPanelAssignmentEntity p " +
           "WHERE p.tenantId = :tenantId " +
           "AND p.providerId = :providerId " +
           "AND p.patientId = :patientId " +
           "AND p.active = true")
    boolean isPatientAssignedToProvider(
            @Param("tenantId") String tenantId,
            @Param("providerId") UUID providerId,
            @Param("patientId") UUID patientId);
}
