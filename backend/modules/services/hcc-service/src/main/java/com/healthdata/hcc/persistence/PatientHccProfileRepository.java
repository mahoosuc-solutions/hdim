package com.healthdata.hcc.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientHccProfileRepository extends JpaRepository<PatientHccProfileEntity, UUID> {

    Optional<PatientHccProfileEntity> findByTenantIdAndPatientIdAndProfileYear(
        String tenantId, UUID patientId, Integer profileYear);

    List<PatientHccProfileEntity> findByTenantIdAndPatientId(String tenantId, UUID patientId);

    @Query("SELECT p FROM PatientHccProfileEntity p WHERE p.tenantId = :tenantId " +
           "AND p.profileYear = :year AND p.potentialRafUplift > :minUplift " +
           "ORDER BY p.potentialRafUplift DESC")
    List<PatientHccProfileEntity> findHighValueOpportunities(
        @Param("tenantId") String tenantId,
        @Param("year") Integer year,
        @Param("minUplift") java.math.BigDecimal minUplift);
}
