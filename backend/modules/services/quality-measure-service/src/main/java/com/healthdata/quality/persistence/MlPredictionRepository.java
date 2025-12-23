package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MlPredictionRepository extends JpaRepository<MlPredictionEntity, UUID> {
}
