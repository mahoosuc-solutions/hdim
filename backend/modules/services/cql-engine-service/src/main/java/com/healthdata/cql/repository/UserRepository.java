package com.healthdata.cql.repository;

import com.healthdata.authentication.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for User entity in CQL Engine Service.
 * <p>
 * Extends the base authentication UserRepository to enable:
 * - User auto-registration via UserAutoRegistrationFilter
 * - Audit tracking of who triggers CQL measure evaluations
 * - Multi-tenant user management
 * - Role-based access control (RBAC)
 * <p>
 * This repository is used by:
 * - UserAutoRegistrationFilter (auto-registers users on first access)
 * - CQL evaluation endpoints (tracks evaluation creators)
 * - Audit logging (associates evaluations with users)
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>,
        com.healthdata.authentication.repository.UserRepository {
    // Inherits all methods from base authentication UserRepository:
    // - findByUsername(String username)
    // - findByEmail(String email)
    // - existsByUsername(String username)
    // - existsByEmail(String email)
    // - findAllByTenantIdsContaining(String tenantId)
}
