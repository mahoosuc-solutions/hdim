package com.healthdata.gateway.admin.operations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OperationRunStepRepository extends JpaRepository<OperationRunStep, UUID> {
    List<OperationRunStep> findByRun_IdOrderByStepOrderAscCreatedAtAsc(UUID runId);
}
