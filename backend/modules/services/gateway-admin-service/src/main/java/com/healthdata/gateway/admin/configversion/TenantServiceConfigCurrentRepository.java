package com.healthdata.gateway.admin.configversion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantServiceConfigCurrentRepository
    extends JpaRepository<TenantServiceConfigCurrent, TenantServiceConfigCurrent.Key> {

    Optional<TenantServiceConfigCurrent> findByTenantIdAndServiceName(String tenantId, String serviceName);
}
