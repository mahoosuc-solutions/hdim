package com.healthdata.agentvalidation.repository;

import com.healthdata.agentvalidation.domain.entity.TestSuite;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import com.healthdata.agentvalidation.domain.enums.UserStoryType;
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
 * Repository for TestSuite entities.
 */
@Repository
public interface TestSuiteRepository extends JpaRepository<TestSuite, UUID> {

    /**
     * Find test suite by ID and tenant.
     */
    Optional<TestSuite> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find all active test suites for a tenant.
     */
    List<TestSuite> findByTenantIdAndActiveTrue(String tenantId);

    /**
     * Find test suites by tenant with pagination.
     */
    Page<TestSuite> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find test suites by user story type.
     */
    List<TestSuite> findByTenantIdAndUserStoryType(String tenantId, UserStoryType userStoryType);

    /**
     * Find test suites by target role.
     */
    List<TestSuite> findByTenantIdAndTargetRole(String tenantId, String targetRole);

    /**
     * Find test suites by agent type.
     */
    List<TestSuite> findByTenantIdAndAgentType(String tenantId, String agentType);

    /**
     * Find test suites by status.
     */
    List<TestSuite> findByTenantIdAndStatus(String tenantId, TestStatus status);

    /**
     * Find test suites that haven't been run recently.
     */
    @Query("SELECT ts FROM TestSuite ts WHERE ts.tenantId = :tenantId " +
           "AND ts.active = true " +
           "AND (ts.lastExecutionAt IS NULL OR ts.lastExecutionAt < :threshold)")
    List<TestSuite> findStaleTestSuites(
        @Param("tenantId") String tenantId,
        @Param("threshold") Instant threshold
    );

    /**
     * Find test suites with failing pass rate.
     */
    @Query("SELECT ts FROM TestSuite ts WHERE ts.tenantId = :tenantId " +
           "AND ts.lastPassRate IS NOT NULL " +
           "AND ts.lastPassRate < ts.passThreshold")
    List<TestSuite> findFailingTestSuites(@Param("tenantId") String tenantId);

    /**
     * Count test suites by status for a tenant.
     */
    @Query("SELECT ts.status, COUNT(ts) FROM TestSuite ts " +
           "WHERE ts.tenantId = :tenantId GROUP BY ts.status")
    List<Object[]> countByStatusForTenant(@Param("tenantId") String tenantId);

    /**
     * Find test suites by name pattern.
     */
    @Query("SELECT ts FROM TestSuite ts WHERE ts.tenantId = :tenantId " +
           "AND LOWER(ts.name) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
    List<TestSuite> findByNameContaining(
        @Param("tenantId") String tenantId,
        @Param("namePattern") String namePattern
    );
}
