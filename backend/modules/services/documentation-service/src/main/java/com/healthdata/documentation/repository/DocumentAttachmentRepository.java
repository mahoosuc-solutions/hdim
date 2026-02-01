package com.healthdata.documentation.repository;

import com.healthdata.documentation.persistence.DocumentAttachmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Full-text search on OCR extracted text using PostgreSQL's full-text search
     * Uses to_tsvector and plainto_tsquery for efficient full-text search
     * Searches for query string in ocrText field with stemming and ranking
     *
     * Note: Requires GIN index on to_tsvector(ocr_text) for optimal performance
     * Created by migration: 0005-add-ocr-fields-to-document-attachments.xml
     */
    @Query(value = "SELECT a.* FROM document_attachments a " +
           "WHERE a.tenant_id = :tenantId " +
           "AND a.ocr_text IS NOT NULL " +
           "AND to_tsvector('english', a.ocr_text) @@ plainto_tsquery('english', :query) " +
           "ORDER BY ts_rank(to_tsvector('english', a.ocr_text), plainto_tsquery('english', :query)) DESC",
           countQuery = "SELECT COUNT(*) FROM document_attachments a " +
           "WHERE a.tenant_id = :tenantId " +
           "AND a.ocr_text IS NOT NULL " +
           "AND to_tsvector('english', a.ocr_text) @@ plainto_tsquery('english', :query)",
           nativeQuery = true)
    Page<DocumentAttachmentEntity> searchOcrText(@Param("tenantId") String tenantId,
                                                   @Param("query") String query,
                                                   Pageable pageable);

    /**
     * Find all attachments with completed OCR status
     */
    @Query("SELECT a FROM DocumentAttachmentEntity a WHERE a.tenantId = :tenantId " +
           "AND a.ocrStatus = 'COMPLETED'")
    List<DocumentAttachmentEntity> findByTenantIdAndOcrCompleted(@Param("tenantId") String tenantId);

    /**
     * Find attachments pending OCR processing
     */
    @Query("SELECT a FROM DocumentAttachmentEntity a WHERE a.tenantId = :tenantId " +
           "AND a.ocrStatus = 'PENDING'")
    List<DocumentAttachmentEntity> findByTenantIdAndOcrPending(@Param("tenantId") String tenantId);
}
