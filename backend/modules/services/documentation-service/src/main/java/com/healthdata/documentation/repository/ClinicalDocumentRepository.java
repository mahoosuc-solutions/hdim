package com.healthdata.documentation.repository;

import com.healthdata.documentation.persistence.ClinicalDocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicalDocumentRepository extends JpaRepository<ClinicalDocumentEntity, UUID> {

    List<ClinicalDocumentEntity> findByTenantId(String tenantId);

    Page<ClinicalDocumentEntity> findByTenantId(String tenantId, Pageable pageable);

    Optional<ClinicalDocumentEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<ClinicalDocumentEntity> findByTenantIdAndPatientId(String tenantId, String patientId);

    Page<ClinicalDocumentEntity> findByTenantIdAndPatientId(String tenantId, String patientId, Pageable pageable);

    List<ClinicalDocumentEntity> findByTenantIdAndStatus(String tenantId, String status);

    List<ClinicalDocumentEntity> findByTenantIdAndDocumentType(String tenantId, String documentType);

    @Query("SELECT d FROM ClinicalDocumentEntity d WHERE d.tenantId = :tenantId AND d.patientId = :patientId AND d.status = 'current'")
    List<ClinicalDocumentEntity> findCurrentDocuments(@Param("tenantId") String tenantId, @Param("patientId") String patientId);

    @Query("SELECT d FROM ClinicalDocumentEntity d WHERE d.tenantId = :tenantId AND d.documentDate BETWEEN :startDate AND :endDate")
    List<ClinicalDocumentEntity> findByDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT d FROM ClinicalDocumentEntity d WHERE d.tenantId = :tenantId AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<ClinicalDocumentEntity> searchDocuments(@Param("tenantId") String tenantId, @Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(d) FROM ClinicalDocumentEntity d WHERE d.tenantId = :tenantId AND d.patientId = :patientId")
    long countByTenantIdAndPatientId(@Param("tenantId") String tenantId, @Param("patientId") String patientId);

    boolean existsByIdAndTenantId(UUID id, String tenantId);
}
