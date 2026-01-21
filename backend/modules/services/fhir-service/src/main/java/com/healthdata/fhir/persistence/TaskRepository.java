package com.healthdata.fhir.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

    Optional<TaskEntity> findByTenantIdAndId(String tenantId, UUID id);

    Page<TaskEntity> findByTenantIdAndPatientIdOrderByAuthoredOnDesc(
            String tenantId, UUID patientId, Pageable pageable);

    @Query("SELECT t FROM TaskEntity t WHERE t.tenantId = :tenantId " +
           "AND t.patientId = :patientId " +
           "AND t.authoredOn BETWEEN :startDate AND :endDate " +
           "ORDER BY t.authoredOn DESC")
    List<TaskEntity> findByPatientAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM TaskEntity t WHERE t.tenantId = :tenantId " +
           "AND t.authoredOn BETWEEN :startDate AND :endDate " +
           "ORDER BY t.authoredOn DESC")
    List<TaskEntity> findByTenantAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
