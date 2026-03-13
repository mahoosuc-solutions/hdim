package com.healthdata.caregap.persistence;

import com.healthdata.caregap.projection.StarRatingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StarRatingSnapshotRepository extends JpaRepository<StarRatingSnapshot, Long> {

    List<StarRatingSnapshot> findByTenantIdAndSnapshotGranularityAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(
        String tenantId,
        String snapshotGranularity,
        LocalDate startDate
    );

    Optional<StarRatingSnapshot> findByTenantIdAndSnapshotDateAndSnapshotGranularity(
        String tenantId,
        LocalDate snapshotDate,
        String snapshotGranularity
    );
}
