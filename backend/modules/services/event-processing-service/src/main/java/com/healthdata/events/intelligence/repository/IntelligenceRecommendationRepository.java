package com.healthdata.events.intelligence.repository;

import com.healthdata.events.intelligence.entity.IntelligenceRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IntelligenceRecommendationRepository extends JpaRepository<IntelligenceRecommendationEntity, UUID> {

    List<IntelligenceRecommendationEntity> findByTenantIdAndPatientRefOrderByCreatedAtDesc(String tenantId, String patientRef);

    java.util.Optional<IntelligenceRecommendationEntity> findByIdAndTenantId(UUID id, String tenantId);
}
