package com.healthdata.documentation.repository;

import com.healthdata.documentation.persistence.DocumentMetadataEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadataEntity, String> {

    List<DocumentMetadataEntity> findByTenantId(String tenantId);

    Page<DocumentMetadataEntity> findByTenantId(String tenantId, Pageable pageable);

    Optional<DocumentMetadataEntity> findByIdAndTenantId(String id, String tenantId);

    List<DocumentMetadataEntity> findByTenantIdAndPortalType(String tenantId, String portalType);

    List<DocumentMetadataEntity> findByTenantIdAndCategory(String tenantId, String category);

    List<DocumentMetadataEntity> findByTenantIdAndStatus(String tenantId, String status);

    @Query("SELECT d FROM DocumentMetadataEntity d WHERE d.tenantId = :tenantId AND d.status = 'published'")
    Page<DocumentMetadataEntity> findPublishedDocuments(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT d FROM DocumentMetadataEntity d WHERE d.tenantId = :tenantId AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.summary) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<DocumentMetadataEntity> searchDocuments(@Param("tenantId") String tenantId, @Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT d.category FROM DocumentMetadataEntity d WHERE d.tenantId = :tenantId")
    List<String> findDistinctCategories(@Param("tenantId") String tenantId);

    @Query("SELECT d FROM DocumentMetadataEntity d WHERE d.tenantId = :tenantId AND d.nextReviewDate <= CURRENT_DATE")
    List<DocumentMetadataEntity> findDocumentsNeedingReview(@Param("tenantId") String tenantId);

    @Modifying
    @Query("UPDATE DocumentMetadataEntity d SET d.viewCount = d.viewCount + 1 WHERE d.id = :id")
    void incrementViewCount(@Param("id") String id);

    boolean existsByIdAndTenantId(String id, String tenantId);

    Optional<DocumentMetadataEntity> findByPath(String path);
}
