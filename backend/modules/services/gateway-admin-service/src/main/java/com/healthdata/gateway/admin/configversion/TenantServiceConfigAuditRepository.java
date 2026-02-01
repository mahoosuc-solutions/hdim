package com.healthdata.gateway.admin.configversion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantServiceConfigAuditRepository extends JpaRepository<TenantServiceConfigAudit, UUID> {

    List<TenantServiceConfigAudit> findByTenantIdAndServiceNameOrderByCreatedAtDesc(
        String tenantId,
        String serviceName
    );
}
