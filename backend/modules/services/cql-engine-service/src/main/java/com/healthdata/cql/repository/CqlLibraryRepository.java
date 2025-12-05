package com.healthdata.cql.repository;

import com.healthdata.cql.entity.CqlLibrary;
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
 * Repository for CQL Library entities
 *
 * Provides multi-tenant access to CQL libraries with query methods
 * for common lookup patterns.
 */
@Repository
public interface CqlLibraryRepository extends JpaRepository<CqlLibrary, UUID> {

    /**
     * Find all libraries for a specific tenant
     */
    List<CqlLibrary> findByTenantIdAndActiveTrue(String tenantId);

    /**
     * Find all libraries for a tenant with pagination
     */
    Page<CqlLibrary> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);

    /**
     * Find a specific library by name and version for a tenant
     */
    Optional<CqlLibrary> findByTenantIdAndLibraryNameAndVersionAndActiveTrue(
            String tenantId, String libraryName, String version);

    /**
     * Find all versions of a library by name for a tenant
     */
    List<CqlLibrary> findByTenantIdAndLibraryNameAndActiveTrueOrderByVersionDesc(
            String tenantId, String libraryName);

    /**
     * Find the latest version of a library by name for a tenant
     */
    @Query("SELECT l FROM CqlLibrary l WHERE l.tenantId = :tenantId " +
           "AND l.libraryName = :libraryName AND l.active = true " +
           "ORDER BY l.version DESC LIMIT 1")
    Optional<CqlLibrary> findLatestVersionByName(
            @Param("tenantId") String tenantId,
            @Param("libraryName") String libraryName);

    /**
     * Find libraries by status for a tenant
     */
    List<CqlLibrary> findByTenantIdAndStatusAndActiveTrue(String tenantId, String status);

    /**
     * Find libraries by publisher for a tenant
     */
    List<CqlLibrary> findByTenantIdAndPublisherAndActiveTrue(String tenantId, String publisher);

    /**
     * Find library by FHIR resource ID
     */
    Optional<CqlLibrary> findByTenantIdAndFhirLibraryIdAndActiveTrue(
            String tenantId, UUID fhirLibraryId);

    /**
     * Search libraries by name pattern (case-insensitive)
     */
    @Query("SELECT l FROM CqlLibrary l WHERE l.tenantId = :tenantId " +
           "AND LOWER(l.libraryName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND l.active = true")
    List<CqlLibrary> searchByName(
            @Param("tenantId") String tenantId,
            @Param("searchTerm") String searchTerm);

    /**
     * Count active libraries for a tenant
     */
    long countByTenantIdAndActiveTrue(String tenantId);

    /**
     * Count libraries by status for a tenant
     */
    long countByTenantIdAndStatusAndActiveTrue(String tenantId, String status);

    /**
     * Check if a library exists by name and version for a tenant
     */
    boolean existsByTenantIdAndLibraryNameAndVersionAndActiveTrue(
            String tenantId, String libraryName, String version);

    /**
     * Find all ACTIVE libraries for a tenant
     */
    @Query("SELECT l FROM CqlLibrary l WHERE l.tenantId = :tenantId " +
           "AND l.status = 'ACTIVE' AND l.active = true")
    List<CqlLibrary> findActiveLibraries(@Param("tenantId") String tenantId);
}
