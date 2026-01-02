package com.healthdata.documentation.repository;

import com.healthdata.documentation.persistence.DocumentVersionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersionEntity, UUID> {

    List<DocumentVersionEntity> findByDocumentIdAndTenantId(String documentId, String tenantId);

    Optional<DocumentVersionEntity> findByIdAndTenantId(UUID id, String tenantId);

    @Query("SELECT v FROM DocumentVersionEntity v WHERE v.documentId = :documentId AND v.tenantId = :tenantId ORDER BY v.createdAt DESC")
    List<DocumentVersionEntity> findVersionsOrderByCreatedAtDesc(@Param("documentId") String documentId, @Param("tenantId") String tenantId);

    @Query("SELECT v FROM DocumentVersionEntity v WHERE v.documentId = :documentId AND v.tenantId = :tenantId ORDER BY v.createdAt DESC")
    List<DocumentVersionEntity> findLatestVersions(@Param("documentId") String documentId, @Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT v FROM DocumentVersionEntity v WHERE v.documentId = :documentId AND v.versionNumber = :versionNumber AND v.tenantId = :tenantId")
    Optional<DocumentVersionEntity> findByDocumentIdAndVersionNumber(
            @Param("documentId") String documentId,
            @Param("versionNumber") String versionNumber,
            @Param("tenantId") String tenantId);

    @Query("SELECT COUNT(v) FROM DocumentVersionEntity v WHERE v.documentId = :documentId")
    long countByDocumentId(@Param("documentId") String documentId);

    @Query("SELECT v FROM DocumentVersionEntity v WHERE v.documentId = :documentId AND v.isMajorVersion = true ORDER BY v.createdAt DESC")
    List<DocumentVersionEntity> findMajorVersions(@Param("documentId") String documentId);
}
