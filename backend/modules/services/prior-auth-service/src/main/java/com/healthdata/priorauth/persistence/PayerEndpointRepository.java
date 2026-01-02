package com.healthdata.priorauth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payer API endpoint configurations.
 */
@Repository
public interface PayerEndpointRepository extends JpaRepository<PayerEndpointEntity, UUID> {

    Optional<PayerEndpointEntity> findByPayerId(String payerId);

    List<PayerEndpointEntity> findByIsActiveTrue();

    @Query("SELECT p FROM PayerEndpointEntity p WHERE p.isActive = true " +
           "AND p.healthStatus = :status")
    List<PayerEndpointEntity> findByHealthStatus(
        @Param("status") PayerEndpointEntity.HealthStatus status);

    @Query("SELECT p FROM PayerEndpointEntity p WHERE p.isActive = true " +
           "AND p.supportsRealTime = true")
    List<PayerEndpointEntity> findRealTimeCapable();

    @Query("SELECT p FROM PayerEndpointEntity p WHERE p.isActive = true " +
           "AND p.payerName LIKE %:name%")
    List<PayerEndpointEntity> searchByPayerName(@Param("name") String name);

    boolean existsByPayerId(String payerId);
}
