package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecutionEntity, UUID> {
}
