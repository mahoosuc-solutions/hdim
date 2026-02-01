package com.healthdata.quality.persistence;

import com.healthdata.authentication.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * User repository for Quality Measure Service.
 * <p>
 * Extends base UserRepository from authentication module to enable:
 * - Audit tracking of quality measure creators
 * - User context for measure evaluations
 * - SMS MFA functionality
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>,
    com.healthdata.authentication.repository.UserRepository {
    // Inherits all methods from base authentication UserRepository
    // Service-specific user queries can be added here if needed
}
