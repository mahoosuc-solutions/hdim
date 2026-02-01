package com.healthdata.quality.persistence;

import com.healthdata.authentication.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Tenant repository for Quality Measure Service.
 * <p>
 * Extends base TenantRepository from authentication module to enable:
 * - Multi-tenant isolation for quality measures
 * - Tenant-level reporting and analytics
 * - Tenant configuration and management
 * </p>
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, String>,
    com.healthdata.authentication.repository.TenantRepository {
    // Inherits all methods from base authentication TenantRepository
    // Service-specific tenant queries can be added here if needed
}
