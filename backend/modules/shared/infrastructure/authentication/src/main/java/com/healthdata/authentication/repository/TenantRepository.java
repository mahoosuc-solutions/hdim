package com.healthdata.authentication.repository;

import com.healthdata.authentication.domain.Tenant;
import com.healthdata.authentication.domain.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Tenant entity operations.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {

    /**
     * Find tenant by ID (case-insensitive).
     */
    Optional<Tenant> findByIdIgnoreCase(String id);

    /**
     * Find all tenants by status.
     */
    List<Tenant> findByStatus(TenantStatus status);

    /**
     * Find all active tenants.
     */
    List<Tenant> findByStatusOrderByNameAsc(TenantStatus status);

    /**
     * Check if tenant exists by ID (case-insensitive).
     */
    boolean existsByIdIgnoreCase(String id);

    /**
     * Find tenant by name (case-insensitive).
     */
    Optional<Tenant> findByNameIgnoreCase(String name);

    /**
     * Check if tenant name exists (case-insensitive).
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Count tenants by status.
     */
    Long countByStatus(TenantStatus status);
}
