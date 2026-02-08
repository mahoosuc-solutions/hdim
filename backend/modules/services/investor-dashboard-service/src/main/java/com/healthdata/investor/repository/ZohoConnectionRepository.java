package com.healthdata.investor.repository;

import com.healthdata.investor.entity.ZohoConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing Zoho ONE OAuth connections
 */
@Repository
public interface ZohoConnectionRepository extends JpaRepository<ZohoConnection, UUID> {

    /**
     * Find Zoho connection by user ID
     */
    @Query("SELECT z FROM ZohoConnection z WHERE z.user.id = :userId")
    Optional<ZohoConnection> findByUserId(@Param("userId") UUID userId);

    /**
     * Find Zoho connection by user ID and tenant ID (multi-tenant isolation)
     */
    @Query("SELECT z FROM ZohoConnection z WHERE z.user.id = :userId AND z.tenantId = :tenantId")
    Optional<ZohoConnection> findByUserIdAndTenantId(
        @Param("userId") UUID userId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find active Zoho connection by user ID
     */
    @Query("SELECT z FROM ZohoConnection z WHERE z.user.id = :userId AND z.connected = true")
    Optional<ZohoConnection> findActiveByUserId(@Param("userId") UUID userId);

    /**
     * Check if user has active Zoho connection
     */
    @Query("SELECT COUNT(z) > 0 FROM ZohoConnection z WHERE z.user.id = :userId AND z.connected = true")
    boolean existsActiveByUserId(@Param("userId") UUID userId);
}
