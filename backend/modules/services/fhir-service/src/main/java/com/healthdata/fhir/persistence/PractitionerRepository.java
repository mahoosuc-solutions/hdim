package com.healthdata.fhir.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PractitionerRepository extends JpaRepository<PractitionerEntity, UUID> {

    Optional<PractitionerEntity> findByTenantIdAndId(String tenantId, UUID id);

    Page<PractitionerEntity> findByTenantId(String tenantId, Pageable pageable);

    List<PractitionerEntity> findByTenantId(String tenantId);

    @Query("SELECT p FROM PractitionerEntity p WHERE p.tenantId = :tenantId " +
           "AND LOWER(p.familyName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<PractitionerEntity> findByTenantIdAndFamilyNameContainingIgnoreCase(
            @Param("tenantId") String tenantId,
            @Param("name") String name);

    @Query("SELECT p FROM PractitionerEntity p WHERE p.tenantId = :tenantId " +
           "AND p.identifierValue = :identifier")
    Optional<PractitionerEntity> findByTenantIdAndIdentifierValue(
            @Param("tenantId") String tenantId,
            @Param("identifier") String identifier);

    long countByTenantId(String tenantId);
}
