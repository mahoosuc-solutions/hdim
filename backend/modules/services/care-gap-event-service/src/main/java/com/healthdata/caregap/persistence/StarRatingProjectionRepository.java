package com.healthdata.caregap.persistence;

import com.healthdata.caregap.projection.StarRatingProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StarRatingProjectionRepository extends JpaRepository<StarRatingProjection, String> {
}
