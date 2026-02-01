package com.healthdata.demo.domain.repository;

import com.healthdata.demo.domain.model.DemoSessionProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for demo session progress tracking.
 */
@Repository
public interface DemoSessionProgressRepository extends JpaRepository<DemoSessionProgress, UUID> {

    Optional<DemoSessionProgress> findBySessionId(UUID sessionId);
}
