package com.healthdata.sdoh.repository;

import com.healthdata.sdoh.entity.ResourceReferralEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceReferralRepository extends JpaRepository<ResourceReferralEntity, String> {

    List<ResourceReferralEntity> findByTenantIdAndPatientId(String tenantId, String patientId);

    @Query("SELECT r FROM ResourceReferralEntity r WHERE r.tenantId = :tenantId AND r.patientId = :patientId " +
           "AND r.status IN ('PENDING', 'CONTACTED', 'SCHEDULED')")
    List<ResourceReferralEntity> findActiveByTenantIdAndPatientId(
            @Param("tenantId") String tenantId,
            @Param("patientId") String patientId);
}
