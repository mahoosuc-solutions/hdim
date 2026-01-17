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

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

    Optional<AppointmentEntity> findByTenantIdAndId(String tenantId, UUID id);

    Page<AppointmentEntity> findByTenantIdAndPatientIdOrderByStartTimeAsc(
            String tenantId, UUID patientId, Pageable pageable);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.tenantId = :tenantId " +
           "AND a.patientId = :patientId " +
           "AND a.startTime BETWEEN :startDate AND :endDate " +
           "ORDER BY a.startTime ASC")
    List<AppointmentEntity> findByPatientAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.tenantId = :tenantId " +
           "AND a.startTime BETWEEN :startDate AND :endDate " +
           "ORDER BY a.startTime ASC")
    List<AppointmentEntity> findByTenantAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
