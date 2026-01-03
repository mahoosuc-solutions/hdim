package com.healthdata.demo.domain.repository;

import com.healthdata.demo.domain.model.DemoSession;
import com.healthdata.demo.domain.model.DemoSession.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DemoSession entity.
 */
@Repository
public interface DemoSessionRepository extends JpaRepository<DemoSession, UUID> {

    /**
     * Find sessions by status.
     */
    List<DemoSession> findByStatusOrderByStartedAtDesc(SessionStatus status);

    /**
     * Find active sessions (not ended).
     */
    @Query("SELECT s FROM DemoSession s WHERE s.status != 'ENDED' ORDER BY s.startedAt DESC")
    List<DemoSession> findActiveSessions();

    /**
     * Find the current active session (most recent non-ended).
     */
    @Query("SELECT s FROM DemoSession s WHERE s.status != 'ENDED' ORDER BY s.startedAt DESC LIMIT 1")
    Optional<DemoSession> findCurrentSession();

    /**
     * Find sessions by scenario.
     */
    List<DemoSession> findByScenarioIdOrderByStartedAtDesc(UUID scenarioId);

    /**
     * Find sessions by creator.
     */
    List<DemoSession> findByCreatedByOrderByStartedAtDesc(String createdBy);

    /**
     * Count active sessions.
     */
    @Query("SELECT COUNT(s) FROM DemoSession s WHERE s.status != 'ENDED'")
    long countActiveSessions();
}
