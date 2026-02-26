package com.healthdata.gateway.admin.operations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OperationValidationGateRepository extends JpaRepository<OperationValidationGate, UUID> {
    List<OperationValidationGate> findByValidationRun_IdOrderByMeasuredAtAscGateKeyAsc(UUID validationRunId);
    void deleteByValidationRun_Id(UUID validationRunId);
}
