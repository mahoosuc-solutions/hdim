package com.healthdata.migration.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.healthdata.migration.persistence.MigrationCheckpointEntity;

/**
 * Repository for migration checkpoint operations
 */
@Repository
public interface MigrationCheckpointRepository extends JpaRepository<MigrationCheckpointEntity, UUID> {

    // Find checkpoints by job
    List<MigrationCheckpointEntity> findByJobIdOrderByCheckpointNumberDesc(UUID jobId);

    // Find latest checkpoint for a job
    @Query("SELECT c FROM MigrationCheckpointEntity c WHERE c.job.id = :jobId " +
           "ORDER BY c.checkpointNumber DESC")
    Optional<MigrationCheckpointEntity> findLatestByJobId(@Param("jobId") UUID jobId);

    // Find checkpoint by job and number
    Optional<MigrationCheckpointEntity> findByJobIdAndCheckpointNumber(UUID jobId, int checkpointNumber);

    // Count checkpoints by job
    long countByJobId(UUID jobId);

    // Delete checkpoints for a job
    @Modifying
    @Query("DELETE FROM MigrationCheckpointEntity c WHERE c.job.id = :jobId")
    int deleteByJobId(@Param("jobId") UUID jobId);

    // Delete old checkpoints keeping only the latest N
    @Modifying
    @Query("DELETE FROM MigrationCheckpointEntity c WHERE c.job.id = :jobId " +
           "AND c.checkpointNumber < (SELECT MAX(c2.checkpointNumber) FROM MigrationCheckpointEntity c2 " +
           "WHERE c2.job.id = :jobId) - :keepCount")
    int deleteOldCheckpoints(@Param("jobId") UUID jobId, @Param("keepCount") int keepCount);
}
