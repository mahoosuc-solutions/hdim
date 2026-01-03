package com.healthdata.demo.domain.repository;

import com.healthdata.demo.domain.model.DemoSnapshot;
import com.healthdata.demo.domain.model.DemoSnapshot.SnapshotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DemoSnapshot entity.
 */
@Repository
public interface DemoSnapshotRepository extends JpaRepository<DemoSnapshot, UUID> {

    /**
     * Find snapshot by name.
     */
    Optional<DemoSnapshot> findByName(String name);

    /**
     * Find all ready snapshots.
     */
    List<DemoSnapshot> findByStatusOrderByCreatedAtDesc(SnapshotStatus status);

    /**
     * Find ready snapshots.
     */
    default List<DemoSnapshot> findReadySnapshots() {
        return findByStatusOrderByCreatedAtDesc(SnapshotStatus.READY);
    }

    /**
     * Find snapshots by scenario.
     */
    List<DemoSnapshot> findByScenarioIdAndStatusOrderByCreatedAtDesc(
            UUID scenarioId, SnapshotStatus status);

    /**
     * Find most recently restored snapshot.
     */
    @Query("SELECT s FROM DemoSnapshot s WHERE s.status = 'READY' " +
           "ORDER BY s.lastRestoredAt DESC NULLS LAST LIMIT 1")
    Optional<DemoSnapshot> findMostRecentlyRestored();

    /**
     * Check if snapshot name exists.
     */
    boolean existsByName(String name);

    /**
     * Find snapshots by creator.
     */
    List<DemoSnapshot> findByCreatedByAndStatusOrderByCreatedAtDesc(
            String createdBy, SnapshotStatus status);

    /**
     * Calculate total snapshot storage.
     */
    @Query("SELECT COALESCE(SUM(s.fileSizeBytes), 0) FROM DemoSnapshot s WHERE s.status = 'READY'")
    Long calculateTotalStorageBytes();

    /**
     * Find old snapshots for cleanup (not restored in X days).
     */
    @Query("SELECT s FROM DemoSnapshot s WHERE s.status = 'READY' " +
           "AND (s.lastRestoredAt IS NULL OR s.lastRestoredAt < :cutoffDate) " +
           "AND s.createdAt < :cutoffDate")
    List<DemoSnapshot> findStaleSnapshots(@Param("cutoffDate") java.time.Instant cutoffDate);
}
