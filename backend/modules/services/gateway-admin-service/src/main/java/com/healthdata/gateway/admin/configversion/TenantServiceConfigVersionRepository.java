package com.healthdata.gateway.admin.configversion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantServiceConfigVersionRepository extends JpaRepository<TenantServiceConfigVersion, UUID> {

    Optional<TenantServiceConfigVersion> findTopByTenantIdAndServiceNameOrderByVersionNumberDesc(
        String tenantId,
        String serviceName
    );

    Optional<TenantServiceConfigVersion> findByIdAndTenantIdAndServiceName(
        UUID id,
        String tenantId,
        String serviceName
    );

    List<TenantServiceConfigVersion> findByTenantIdAndServiceNameOrderByVersionNumberDesc(
        String tenantId,
        String serviceName
    );
}
