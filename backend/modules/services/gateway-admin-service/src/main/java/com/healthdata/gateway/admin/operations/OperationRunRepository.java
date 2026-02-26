package com.healthdata.gateway.admin.operations;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationRunRepository extends JpaRepository<OperationRun, UUID> {
    List<OperationRun> findAllByOrderByRequestedAtDesc(Pageable pageable);
    long countByStatus(OperationRun.RunStatus status);
    Optional<OperationRun> findTopByOperationTypeOrderByRequestedAtDesc(OperationRun.OperationType operationType);
    boolean existsByStatusAndOperationTypeIn(
        OperationRun.RunStatus status,
        List<OperationRun.OperationType> operationTypes
    );
    Optional<OperationRun> findTopByOperationTypeAndIdempotencyKeyOrderByRequestedAtDesc(
        OperationRun.OperationType operationType,
        String idempotencyKey
    );
}
