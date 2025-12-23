package com.healthdata.caregap.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CareGapRecommendationRepository extends JpaRepository<CareGapRecommendationEntity, UUID> {
}
