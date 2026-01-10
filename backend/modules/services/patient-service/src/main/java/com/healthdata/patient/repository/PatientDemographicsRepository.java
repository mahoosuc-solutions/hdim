package com.healthdata.patient.repository;

import com.healthdata.patient.entity.PatientDemographicsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Patient Demographics
 *
 * Provides multi-tenant queries for patient demographic data.
 */
public interface PatientDemographicsRepository extends JpaRepository<PatientDemographicsEntity, UUID> {

    /**
     * Find patient by ID and tenant ID for multi-tenant isolation.
     */
    Optional<PatientDemographicsEntity> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find patient by FHIR patient ID and tenant ID.
     */
    Optional<PatientDemographicsEntity> findByFhirPatientIdAndTenantId(String fhirPatientId, String tenantId);

    /**
     * Find all active patients for a tenant.
     */
    @Query("SELECT p FROM PatientDemographicsEntity p WHERE p.tenantId = :tenantId AND p.active = true")
    List<PatientDemographicsEntity> findActiveByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find patients by PCP ID for a tenant.
     */
    @Query("SELECT p FROM PatientDemographicsEntity p WHERE p.tenantId = :tenantId AND p.pcpId = :pcpId AND p.active = true")
    List<PatientDemographicsEntity> findByPcpIdAndTenantId(@Param("pcpId") String pcpId, @Param("tenantId") String tenantId);

    /**
     * Find active patients for a tenant with pagination.
     */
    Page<PatientDemographicsEntity> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);
}
