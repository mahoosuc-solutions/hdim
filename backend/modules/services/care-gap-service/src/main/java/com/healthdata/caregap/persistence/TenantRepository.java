package com.healthdata.caregap.persistence;

import com.healthdata.authentication.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Tenant repository for Care Gap Service.
 * <p>
 * Extends base TenantRepository from authentication module to enable:
 * - Multi-tenant isolation for care gaps
 * - Tenant-level care gap analytics
 * - Tenant configuration and management
 * </p>
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, String>,
    com.healthdata.authentication.repository.TenantRepository {
    // Inherits all methods from base authentication TenantRepository
    // Service-specific tenant queries can be added here if needed
}
