package com.healthdata.analytics.repository;

import com.healthdata.analytics.persistence.StarRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StarRatingRepository extends JpaRepository<StarRatingEntity, UUID> {
}
