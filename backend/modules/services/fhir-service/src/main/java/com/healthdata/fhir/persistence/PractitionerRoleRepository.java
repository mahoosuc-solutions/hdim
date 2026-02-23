package com.healthdata.fhir.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PractitionerRoleRepository extends JpaRepository<PractitionerRoleEntity, UUID> {

    Optional<PractitionerRoleEntity> findByTenantIdAndId(String tenantId, UUID id);

    Page<PractitionerRoleEntity> findByTenantId(String tenantId, Pageable pageable);

    List<PractitionerRoleEntity> findByTenantId(String tenantId);

    List<PractitionerRoleEntity> findByTenantIdAndPractitionerId(String tenantId, String practitionerId);

    List<PractitionerRoleEntity> findByTenantIdAndRoleCode(String tenantId, String roleCode);

    @Query("SELECT r FROM PractitionerRoleEntity r WHERE r.tenantId = :tenantId " +
           "AND r.identifierValue = :identifier")
    Optional<PractitionerRoleEntity> findByTenantIdAndIdentifierValue(
            @Param("tenantId") String tenantId,
            @Param("identifier") String identifier);

    long countByTenantId(String tenantId);
}
