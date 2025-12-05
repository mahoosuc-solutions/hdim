package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Care Team Member Repository
 *
 * Provides query methods for accessing patient care team data.
 */
@Repository
public interface CareTeamMemberRepository extends JpaRepository<CareTeamMemberEntity, String> {

    /**
     * Find all active care team members for a patient
     */
    @Query("SELECT c FROM CareTeamMemberEntity c WHERE c.patientId = :patientId " +
           "AND c.tenantId = :tenantId AND c.active = true " +
           "AND (c.endDate IS NULL OR c.endDate > CURRENT_TIMESTAMP)")
    List<CareTeamMemberEntity> findActiveByPatientIdAndTenantId(
        @Param("patientId") String patientId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find primary care team member for a patient
     */
    @Query("SELECT c FROM CareTeamMemberEntity c WHERE c.patientId = :patientId " +
           "AND c.tenantId = :tenantId AND c.active = true AND c.isPrimary = true " +
           "AND (c.endDate IS NULL OR c.endDate > CURRENT_TIMESTAMP)")
    Optional<CareTeamMemberEntity> findPrimaryByPatientIdAndTenantId(
        @Param("patientId") String patientId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all care team members by user ID
     */
    List<CareTeamMemberEntity> findByUserIdAndTenantId(String userId, String tenantId);

    /**
     * Find active care team members by role for a patient
     */
    @Query("SELECT c FROM CareTeamMemberEntity c WHERE c.patientId = :patientId " +
           "AND c.tenantId = :tenantId AND c.role = :role AND c.active = true " +
           "AND (c.endDate IS NULL OR c.endDate > CURRENT_TIMESTAMP)")
    List<CareTeamMemberEntity> findActiveByPatientIdAndTenantIdAndRole(
        @Param("patientId") String patientId,
        @Param("tenantId") String tenantId,
        @Param("role") CareTeamMemberEntity.CareTeamRole role
    );

    /**
     * Count active care team members for a patient
     */
    @Query("SELECT COUNT(c) FROM CareTeamMemberEntity c WHERE c.patientId = :patientId " +
           "AND c.tenantId = :tenantId AND c.active = true " +
           "AND (c.endDate IS NULL OR c.endDate > CURRENT_TIMESTAMP)")
    long countActiveByPatientIdAndTenantId(
        @Param("patientId") String patientId,
        @Param("tenantId") String tenantId
    );
}
