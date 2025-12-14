package com.healthdata.fhir.security.smart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SMART on FHIR client registrations.
 */
@Repository
public interface SmartClientRepository extends JpaRepository<SmartClient, UUID> {

    /**
     * Find client by client ID.
     */
    Optional<SmartClient> findByClientId(String clientId);

    /**
     * Find active client by client ID.
     */
    Optional<SmartClient> findByClientIdAndActiveTrue(String clientId);

    /**
     * Find all clients for a tenant.
     */
    List<SmartClient> findByTenantId(String tenantId);

    /**
     * Find all active clients for a tenant.
     */
    List<SmartClient> findByTenantIdAndActiveTrue(String tenantId);

    /**
     * Check if client exists by client ID.
     */
    boolean existsByClientId(String clientId);

    /**
     * Find clients by name pattern.
     */
    @Query("SELECT c FROM SmartClient c WHERE c.clientName LIKE %:name% AND c.active = true")
    List<SmartClient> findByClientNameContaining(@Param("name") String name);

    /**
     * Find confidential clients for a tenant.
     */
    List<SmartClient> findByTenantIdAndClientTypeAndActiveTrue(
        String tenantId,
        SmartClient.ClientType clientType
    );

    /**
     * Count active clients for a tenant.
     */
    long countByTenantIdAndActiveTrue(String tenantId);
}
