package com.healthdata.gateway.admin.configversion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TenantServiceConfigApprovalRepository extends JpaRepository<TenantServiceConfigApproval, UUID> {

    List<TenantServiceConfigApproval> findByTenantIdAndServiceNameAndVersionIdOrderByCreatedAtAsc(
        String tenantId,
        String serviceName,
        UUID versionId
    );

    boolean existsByTenantIdAndServiceNameAndVersionIdAndActorAndAction(
        String tenantId,
        String serviceName,
        UUID versionId,
        String actor,
        TenantServiceConfigApproval.Action action
    );

    boolean existsByTenantIdAndServiceNameAndVersionIdAndAction(
        String tenantId,
        String serviceName,
        UUID versionId,
        TenantServiceConfigApproval.Action action
    );

    @Query("select count(distinct a.actor) from TenantServiceConfigApproval a " +
        "where a.tenantId = :tenantId and a.serviceName = :serviceName and a.versionId = :versionId " +
        "and a.action = :action")
    long countDistinctActors(
        @Param("tenantId") String tenantId,
        @Param("serviceName") String serviceName,
        @Param("versionId") UUID versionId,
        @Param("action") TenantServiceConfigApproval.Action action
    );
}
