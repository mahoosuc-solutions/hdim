package com.healthdata.fhir.persistence;

import com.healthdata.authentication.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Tenant repository for FHIR Service.
 * <p>
 * Extends base TenantRepository from authentication module to enable:
 * - Multi-tenant isolation for FHIR resources
 * - SMART on FHIR tenant configuration
 * - Tenant-level FHIR resource access control
 * </p>
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, String>,
    com.healthdata.authentication.repository.TenantRepository {
    // Inherits all methods from base authentication TenantRepository
    // Service-specific tenant queries can be added here if needed
}
