package com.healthdata.investor.repository;

import com.healthdata.investor.entity.LinkedInConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LinkedInConnection entity.
 */
@Repository
public interface LinkedInConnectionRepository extends JpaRepository<LinkedInConnection, UUID> {

    Optional<LinkedInConnection> findByUserId(UUID userId);

    Optional<LinkedInConnection> findByLinkedInMemberId(String linkedInMemberId);

    boolean existsByUserId(UUID userId);
}
