package com.healthdata.payer.repository;

import com.healthdata.payer.domain.PilotReadiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PilotReadinessRepository extends JpaRepository<PilotReadiness, String> {

    Optional<PilotReadiness> findByCustomerIdAndTenantId(String customerId, String tenantId);

    List<PilotReadiness> findByTenantIdOrderByReadinessScoreDesc(String tenantId);

    List<PilotReadiness> findByTenantIdAndIntegrationStatus(
            String tenantId, PilotReadiness.IntegrationStatus status);
}
