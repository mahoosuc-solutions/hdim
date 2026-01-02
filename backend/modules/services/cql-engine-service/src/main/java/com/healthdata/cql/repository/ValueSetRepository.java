package com.healthdata.cql.repository;

import com.healthdata.cql.entity.ValueSet;
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
 * Repository for ValueSet entities
 *
 * Provides multi-tenant access to SNOMED, LOINC, RxNorm, and other
 * code system value sets used in CQL expression evaluation.
 */
@Repository
public interface ValueSetRepository extends JpaRepository<ValueSet, UUID> {

    /**
     * Find all active value sets for a tenant
     */
    List<ValueSet> findByTenantIdAndActiveTrue(String tenantId);

    /**
     * Find all value sets for a tenant with pagination
     */
    Page<ValueSet> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);

    /**
     * Find value set by OID (Object Identifier)
     */
    Optional<ValueSet> findByTenantIdAndOidAndActiveTrue(String tenantId, String oid);

    /**
     * Find value sets by code system
     */
    List<ValueSet> findByTenantIdAndCodeSystemAndActiveTrue(String tenantId, String codeSystem);

    /**
     * Find value sets by code system with pagination
     */
    Page<ValueSet> findByTenantIdAndCodeSystemAndActiveTrue(
            String tenantId, String codeSystem, Pageable pageable);

    /**
     * Find value set by name
     */
    Optional<ValueSet> findByTenantIdAndNameAndActiveTrue(String tenantId, String name);

    /**
     * Find value sets by name pattern (case-insensitive search)
     */
    @Query("SELECT v FROM ValueSet v WHERE v.tenantId = :tenantId " +
           "AND LOWER(v.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND v.active = true")
    List<ValueSet> searchByName(
            @Param("tenantId") String tenantId,
            @Param("searchTerm") String searchTerm);

    /**
     * Find value set by OID and version
     */
    Optional<ValueSet> findByTenantIdAndOidAndVersionAndActiveTrue(
            String tenantId, String oid, String version);

    /**
     * Find value sets by status
     */
    List<ValueSet> findByTenantIdAndStatusAndActiveTrue(String tenantId, String status);

    /**
     * Find value sets by publisher
     */
    List<ValueSet> findByTenantIdAndPublisherAndActiveTrue(String tenantId, String publisher);

    /**
     * Find value set by FHIR ValueSet resource ID
     */
    Optional<ValueSet> findByTenantIdAndFhirValueSetIdAndActiveTrue(
            String tenantId, UUID fhirValueSetId);

    /**
     * Find all versions of a value set by OID
     */
    @Query("SELECT v FROM ValueSet v WHERE v.tenantId = :tenantId " +
           "AND v.oid = :oid AND v.active = true " +
           "ORDER BY v.version DESC")
    List<ValueSet> findAllVersionsByOid(
            @Param("tenantId") String tenantId,
            @Param("oid") String oid);

    /**
     * Find the latest version of a value set by OID
     */
    @Query("SELECT v FROM ValueSet v WHERE v.tenantId = :tenantId " +
           "AND v.oid = :oid AND v.active = true " +
           "ORDER BY v.version DESC LIMIT 1")
    Optional<ValueSet> findLatestVersionByOid(
            @Param("tenantId") String tenantId,
            @Param("oid") String oid);

    /**
     * Find ACTIVE value sets (status = ACTIVE)
     */
    @Query("SELECT v FROM ValueSet v WHERE v.tenantId = :tenantId " +
           "AND v.status = 'ACTIVE' AND v.active = true")
    List<ValueSet> findActiveValueSets(@Param("tenantId") String tenantId);

    /**
     * Find value sets by code system and status
     */
    List<ValueSet> findByTenantIdAndCodeSystemAndStatusAndActiveTrue(
            String tenantId, String codeSystem, String status);

    /**
     * Check if a value set exists by OID
     */
    boolean existsByTenantIdAndOidAndActiveTrue(String tenantId, String oid);

    /**
     * Check if a value set exists by name
     */
    boolean existsByTenantIdAndNameAndActiveTrue(String tenantId, String name);

    /**
     * Count value sets by code system
     */
    long countByTenantIdAndCodeSystemAndActiveTrue(String tenantId, String codeSystem);

    /**
     * Count value sets by status
     */
    long countByTenantIdAndStatusAndActiveTrue(String tenantId, String status);

    /**
     * Count all active value sets for a tenant
     */
    long countByTenantIdAndActiveTrue(String tenantId);

    /**
     * Search value sets by OID pattern
     */
    @Query("SELECT v FROM ValueSet v WHERE v.tenantId = :tenantId " +
           "AND v.oid LIKE CONCAT(:oidPrefix, '%') " +
           "AND v.active = true")
    List<ValueSet> findByOidPrefix(
            @Param("tenantId") String tenantId,
            @Param("oidPrefix") String oidPrefix);

    /**
     * Find value sets for commonly used code systems (SNOMED, LOINC, RxNorm)
     */
    @Query("SELECT v FROM ValueSet v WHERE v.tenantId = :tenantId " +
           "AND v.codeSystem IN ('SNOMED', 'LOINC', 'RxNorm') " +
           "AND v.active = true " +
           "ORDER BY v.codeSystem, v.name")
    List<ValueSet> findCommonCodeSystemValueSets(@Param("tenantId") String tenantId);
}
