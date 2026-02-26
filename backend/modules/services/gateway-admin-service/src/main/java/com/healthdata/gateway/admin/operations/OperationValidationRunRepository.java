package com.healthdata.gateway.admin.operations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OperationValidationRunRepository extends JpaRepository<OperationValidationRun, UUID> {
    Optional<OperationValidationRun> findByOperationRun_Id(UUID runId);
}
