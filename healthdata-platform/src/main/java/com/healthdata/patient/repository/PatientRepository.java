package com.healthdata.patient.repository;

import com.healthdata.patient.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Patient Repository - JPA interface for patient data access
 *
 * Using Spring Data JPA for automatic query generation
 * Custom queries use JPQL for complex operations
 * Comprehensive repository with tenant isolation and pagination support
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {

    /**
     * Find patient by Medical Record Number
     */
    Optional<Patient> findByMrn(String mrn);

    /**
     * Check if patient exists by MRN
     */
    boolean existsByMrn(String mrn);

    /**
     * Find all patients for a tenant
     */
    Page<Patient> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Count active patients by tenant
     */
    long countByTenantIdAndActive(String tenantId, boolean active);

    /**
     * Find patient by MRN and TenantId - ensures tenant isolation
     *
     * @param mrn Medical Record Number
     * @param tenantId Tenant identifier
     * @return Optional containing patient if found
     */
    @Query("""
        SELECT p FROM Patient p
        WHERE p.mrn = :mrn
        AND p.tenantId = :tenantId
        """)
    Optional<Patient> findByMrnAndTenantId(
        @Param("mrn") String mrn,
        @Param("tenantId") String tenantId
    );

    /**
     * Find patient by first name, last name and tenant
     *
     * @param firstName Patient first name
     * @param lastName Patient last name
     * @param tenantId Tenant identifier
     * @return List of matching patients
     */
    @Query("""
        SELECT p FROM Patient p
        WHERE LOWER(p.firstName) = LOWER(:firstName)
        AND LOWER(p.lastName) = LOWER(:lastName)
        AND p.tenantId = :tenantId
        """)
    List<Patient> findByFirstNameAndLastNameAndTenantId(
        @Param("firstName") String firstName,
        @Param("lastName") String lastName,
        @Param("tenantId") String tenantId
    );

    /**
     * Search patients with multiple criteria - highly flexible search
     * Searches by firstName, lastName, and MRN with tenant isolation
     *
     * @param firstName Patient first name (partial match allowed)
     * @param lastName Patient last name (partial match allowed)
     * @param mrn Medical Record Number (partial match allowed)
     * @param tenantId Tenant identifier
     * @param pageable Pagination information
     * @return Paginated results of matching patients
     */
    @Query("""
        SELECT p FROM Patient p
        WHERE p.tenantId = :tenantId
        AND p.active = true
        AND (
            LOWER(p.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))
            OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))
            OR LOWER(p.mrn) LIKE LOWER(CONCAT('%', :mrn, '%'))
        )
        ORDER BY p.lastName, p.firstName
        """)
    Page<Patient> searchPatients(
        @Param("firstName") String firstName,
        @Param("lastName") String lastName,
        @Param("mrn") String mrn,
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    /**
     * Find all active patients for a specific tenant with pagination
     *
     * @param tenantId Tenant identifier
     * @param pageable Pagination information
     * @return Paginated list of active patients
     */
    @Query("""
        SELECT p FROM Patient p
        WHERE p.tenantId = :tenantId
        AND p.active = true
        ORDER BY p.lastName, p.firstName
        """)
    Page<Patient> findAllActivePatientsForTenant(
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    /**
     * Count active patients by tenant
     * Provides high-level metrics for tenant's patient population
     *
     * @param tenantId Tenant identifier
     * @return Total count of active patients
     */
    @Query("""
        SELECT COUNT(p) FROM Patient p
        WHERE p.tenantId = :tenantId
        AND p.active = true
        """)
    long countActivePatientsByTenant(@Param("tenantId") String tenantId);

    /**
     * Search patients by name or MRN for a specific tenant
     */
    @Query("""
        SELECT p FROM Patient p
        WHERE p.tenantId = :tenantId
        AND p.active = true
        AND (
            LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(p.mrn) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        """)
    Page<Patient> searchByTenantAndQuery(
        @Param("tenantId") String tenantId,
        @Param("query") String query,
        Pageable pageable
    );

    /**
     * Find patients with recent activity
     */
    @Query("""
        SELECT p FROM Patient p
        WHERE p.tenantId = :tenantId
        AND p.active = true
        AND p.updatedAt > :thirtyDaysAgo
        ORDER BY p.updatedAt DESC
        """)
    List<Patient> findRecentlyActivePatients(
        @Param("tenantId") String tenantId,
        @Param("thirtyDaysAgo") java.time.Instant thirtyDaysAgo,
        Pageable pageable
    );

    /**
     * Find patients by age range for population health
     * Note: Age calculation deferred to service layer due to JPQL limitations
     */
    @Query("""
        SELECT p FROM Patient p
        WHERE p.tenantId = :tenantId
        AND p.active = true
        AND p.dateOfBirth IS NOT NULL
        ORDER BY p.dateOfBirth DESC
        """)
    List<Patient> findActivePatientsWithDob(
        @Param("tenantId") String tenantId
    );

    /**
     * Bulk find patients by IDs
     */
    @Query("SELECT p FROM Patient p WHERE p.id IN :ids")
    List<Patient> findAllByIds(@Param("ids") List<String> ids);
}