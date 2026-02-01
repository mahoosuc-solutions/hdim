package com.healthdata.cql.repository;

import com.healthdata.authentication.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Tenant entity in CQL Engine Service.
 * <p>
 * Extends the base authentication TenantRepository to enable:
 * - Tenant validation in UserAutoRegistrationFilter
 * - Multi-tenant CQL library and evaluation management
 * - Tenant-scoped access control
 * <p>
 * This repository is used by:
 * - UserAutoRegistrationFilter (validates tenant existence during user registration)
 * - CQL evaluation endpoints (enforces tenant isolation)
 * - Tenant-specific library management
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, String>,
        com.healthdata.authentication.repository.TenantRepository {
    // Inherits all methods from base authentication TenantRepository:
    // - findByName(String name)
    // - existsByName(String name)
    // - findAllByActiveTrue()
}
