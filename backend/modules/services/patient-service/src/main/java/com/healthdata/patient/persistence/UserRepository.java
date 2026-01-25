package com.healthdata.patient.persistence;

import com.healthdata.authentication.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * User repository for Patient Service.
 * <p>
 * Extends base UserRepository from authentication module to enable:
 * - Audit tracking of patient record creators
 * - User context for patient data access
 * - SMS MFA functionality
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>,
    com.healthdata.authentication.repository.UserRepository {
    // Inherits all methods from base authentication UserRepository
    // Service-specific user queries can be added here if needed
}
