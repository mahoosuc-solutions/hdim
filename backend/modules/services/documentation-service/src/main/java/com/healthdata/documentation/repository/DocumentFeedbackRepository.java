package com.healthdata.documentation.repository;

import com.healthdata.documentation.persistence.DocumentFeedbackEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentFeedbackRepository extends JpaRepository<DocumentFeedbackEntity, UUID> {

    List<DocumentFeedbackEntity> findByDocumentIdAndTenantId(String documentId, String tenantId);

    Page<DocumentFeedbackEntity> findByDocumentIdAndTenantId(String documentId, String tenantId, Pageable pageable);

    Optional<DocumentFeedbackEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<DocumentFeedbackEntity> findByTenantIdAndStatus(String tenantId, String status);

    @Query("SELECT f FROM DocumentFeedbackEntity f WHERE f.documentId = :documentId AND f.userId = :userId AND f.tenantId = :tenantId")
    Optional<DocumentFeedbackEntity> findByDocumentIdAndUserId(
            @Param("documentId") String documentId,
            @Param("userId") String userId,
            @Param("tenantId") String tenantId);

    @Query("SELECT AVG(f.rating) FROM DocumentFeedbackEntity f WHERE f.documentId = :documentId AND f.rating IS NOT NULL")
    Double calculateAverageRating(@Param("documentId") String documentId);

    @Query("SELECT COUNT(f) FROM DocumentFeedbackEntity f WHERE f.documentId = :documentId")
    long countByDocumentId(@Param("documentId") String documentId);

    @Query("SELECT COUNT(f) FROM DocumentFeedbackEntity f WHERE f.documentId = :documentId AND f.helpful = true")
    long countHelpfulFeedback(@Param("documentId") String documentId);

    @Query("SELECT f FROM DocumentFeedbackEntity f WHERE f.tenantId = :tenantId AND f.status = 'PENDING' ORDER BY f.createdAt ASC")
    List<DocumentFeedbackEntity> findPendingFeedback(@Param("tenantId") String tenantId, Pageable pageable);
}
