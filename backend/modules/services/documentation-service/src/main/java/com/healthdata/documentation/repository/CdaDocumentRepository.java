package com.healthdata.documentation.repository;

import com.healthdata.documentation.persistence.CdaDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CdaDocumentRepository extends JpaRepository<CdaDocumentEntity, UUID> {

    Optional<CdaDocumentEntity> findByIdAndTenantId(UUID id, String tenantId);

    Optional<CdaDocumentEntity> findByClinicalDocumentIdAndTenantId(UUID clinicalDocumentId, String tenantId);

    List<CdaDocumentEntity> findByTenantIdAndCdaType(String tenantId, String cdaType);

    List<CdaDocumentEntity> findByTenantIdAndValidationStatus(String tenantId, String validationStatus);

    @Query("SELECT c FROM CdaDocumentEntity c WHERE c.tenantId = :tenantId AND c.setId = :setId ORDER BY c.versionNumber DESC")
    List<CdaDocumentEntity> findVersionsBySetId(@Param("tenantId") String tenantId, @Param("setId") String setId);

    @Query("SELECT c FROM CdaDocumentEntity c WHERE c.tenantId = :tenantId AND c.validationStatus = 'INVALID'")
    List<CdaDocumentEntity> findInvalidDocuments(@Param("tenantId") String tenantId);

    boolean existsByIdAndTenantId(UUID id, String tenantId);
}
