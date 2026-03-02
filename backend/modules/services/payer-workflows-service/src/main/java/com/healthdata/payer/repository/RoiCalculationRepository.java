package com.healthdata.payer.repository;

import com.healthdata.payer.domain.RoiCalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoiCalculationRepository extends JpaRepository<RoiCalculation, String> {

    Optional<RoiCalculation> findByIdAndTenantId(String id, String tenantId);

    Page<RoiCalculation> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<RoiCalculation> findByContactEmailOrderByCreatedAtDesc(String contactEmail, Pageable pageable);
}
