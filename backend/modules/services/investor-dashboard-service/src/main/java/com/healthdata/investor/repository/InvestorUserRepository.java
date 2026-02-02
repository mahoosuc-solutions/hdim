package com.healthdata.investor.repository;

import com.healthdata.investor.entity.InvestorUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for InvestorUser entity.
 */
@Repository
public interface InvestorUserRepository extends JpaRepository<InvestorUser, UUID> {

    Optional<InvestorUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
