package com.healthdata.fhir.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

    Optional<OrganizationEntity> findByTenantIdAndId(String tenantId, UUID id);

    Page<OrganizationEntity> findByTenantId(String tenantId, Pageable pageable);

    List<OrganizationEntity> findByTenantId(String tenantId);

    @Query("SELECT o FROM OrganizationEntity o WHERE o.tenantId = :tenantId " +
           "AND LOWER(o.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<OrganizationEntity> findByTenantIdAndNameContainingIgnoreCase(
            @Param("tenantId") String tenantId,
            @Param("name") String name);

    @Query("SELECT o FROM OrganizationEntity o WHERE o.tenantId = :tenantId " +
           "AND o.identifierValue = :identifier")
    Optional<OrganizationEntity> findByTenantIdAndIdentifierValue(
            @Param("tenantId") String tenantId,
            @Param("identifier") String identifier);

    long countByTenantId(String tenantId);
}
