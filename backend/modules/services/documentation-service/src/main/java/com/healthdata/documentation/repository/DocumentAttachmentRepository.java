package com.healthdata.documentation.repository;

import com.healthdata.documentation.persistence.DocumentAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentAttachmentRepository extends JpaRepository<DocumentAttachmentEntity, UUID> {

    Optional<DocumentAttachmentEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<DocumentAttachmentEntity> findByClinicalDocumentIdAndTenantId(UUID clinicalDocumentId, String tenantId);

    List<DocumentAttachmentEntity> findByTenantIdAndContentType(String tenantId, String contentType);

    @Query("SELECT SUM(a.fileSize) FROM DocumentAttachmentEntity a WHERE a.clinicalDocumentId = :clinicalDocumentId")
    Long getTotalFileSizeForDocument(@Param("clinicalDocumentId") UUID clinicalDocumentId);

    @Modifying
    @Query("DELETE FROM DocumentAttachmentEntity a WHERE a.clinicalDocumentId = :clinicalDocumentId AND a.tenantId = :tenantId")
    void deleteByClinicalDocumentIdAndTenantId(@Param("clinicalDocumentId") UUID clinicalDocumentId, @Param("tenantId") String tenantId);

    @Query("SELECT COUNT(a) FROM DocumentAttachmentEntity a WHERE a.clinicalDocumentId = :clinicalDocumentId")
    long countByClinicalDocumentId(@Param("clinicalDocumentId") UUID clinicalDocumentId);

    boolean existsByIdAndTenantId(UUID id, String tenantId);
}
