package com.healthdata.fhir.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for FHIR DocumentReference resources.
 * Provides tenant-scoped queries for clinical document references.
 */
@Repository
public interface DocumentReferenceRepository extends JpaRepository<DocumentReferenceEntity, UUID> {

    /**
     * Find document reference by tenant and ID
     */
    Optional<DocumentReferenceEntity> findByTenantIdAndIdAndDeletedAtIsNull(String tenantId, UUID id);

    /**
     * Find all document references for a patient
     */
    List<DocumentReferenceEntity> findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(
            String tenantId, UUID patientId);

    /**
     * Find document references by patient and status
     */
    List<DocumentReferenceEntity> findByTenantIdAndPatientIdAndStatusAndDeletedAtIsNull(
            String tenantId, UUID patientId, String status);

    /**
     * Find current document references for a patient
     */
    @Query("SELECT d FROM DocumentReferenceEntity d WHERE d.tenantId = :tenantId " +
           "AND d.patientId = :patientId " +
           "AND d.status = 'current' " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.createdDate DESC")
    List<DocumentReferenceEntity> findCurrentDocumentsForPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find document references by encounter
     */
    List<DocumentReferenceEntity> findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByCreatedDateDesc(
            String tenantId, UUID encounterId);

    /**
     * Find document references by type
     */
    List<DocumentReferenceEntity> findByTenantIdAndPatientIdAndTypeCodeAndDeletedAtIsNull(
            String tenantId, UUID patientId, String typeCode);

    /**
     * Find document references by category
     */
    List<DocumentReferenceEntity> findByTenantIdAndPatientIdAndCategoryCodeAndDeletedAtIsNull(
            String tenantId, UUID patientId, String categoryCode);

    /**
     * Search document references with multiple criteria
     */
    @Query("SELECT d FROM DocumentReferenceEntity d WHERE d.tenantId = :tenantId " +
           "AND d.deletedAt IS NULL " +
           "AND (:patientId IS NULL OR d.patientId = :patientId) " +
           "AND (:encounterId IS NULL OR d.encounterId = :encounterId) " +
           "AND (:status IS NULL OR d.status = :status) " +
           "AND (:typeCode IS NULL OR d.typeCode = :typeCode) " +
           "AND (:categoryCode IS NULL OR d.categoryCode = :categoryCode) " +
           "AND (:contentType IS NULL OR d.contentType = :contentType) " +
           "ORDER BY d.createdDate DESC")
    Page<DocumentReferenceEntity> searchDocuments(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("encounterId") UUID encounterId,
            @Param("status") String status,
            @Param("typeCode") String typeCode,
            @Param("categoryCode") String categoryCode,
            @Param("contentType") String contentType,
            Pageable pageable);

    /**
     * Find documents created within a date range
     */
    @Query("SELECT d FROM DocumentReferenceEntity d WHERE d.tenantId = :tenantId " +
           "AND d.patientId = :patientId " +
           "AND d.createdDate BETWEEN :startDate AND :endDate " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.createdDate DESC")
    List<DocumentReferenceEntity> findByCreatedDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find documents indexed within a date range
     */
    @Query("SELECT d FROM DocumentReferenceEntity d WHERE d.tenantId = :tenantId " +
           "AND d.indexedDate BETWEEN :startDate AND :endDate " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.indexedDate DESC")
    List<DocumentReferenceEntity> findByIndexedDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find related documents (that replace/transform this document)
     */
    List<DocumentReferenceEntity> findByTenantIdAndRelatesToTargetAndDeletedAtIsNull(
            String tenantId, String relatesToTarget);

    /**
     * Find documents by author
     */
    List<DocumentReferenceEntity> findByTenantIdAndAuthorReferenceAndDeletedAtIsNull(
            String tenantId, String authorReference);

    /**
     * Count documents by type for a patient
     */
    @Query("SELECT d.typeCode, d.typeDisplay, COUNT(d) FROM DocumentReferenceEntity d " +
           "WHERE d.tenantId = :tenantId AND d.patientId = :patientId AND d.deletedAt IS NULL " +
           "GROUP BY d.typeCode, d.typeDisplay " +
           "ORDER BY COUNT(d) DESC")
    List<Object[]> countByType(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Count documents by category for a patient
     */
    @Query("SELECT d.categoryCode, d.categoryDisplay, COUNT(d) FROM DocumentReferenceEntity d " +
           "WHERE d.tenantId = :tenantId AND d.patientId = :patientId AND d.deletedAt IS NULL " +
           "GROUP BY d.categoryCode, d.categoryDisplay " +
           "ORDER BY COUNT(d) DESC")
    List<Object[]> countByCategory(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find latest document of a specific type for a patient
     */
    @Query("SELECT d FROM DocumentReferenceEntity d WHERE d.tenantId = :tenantId " +
           "AND d.patientId = :patientId " +
           "AND d.typeCode = :typeCode " +
           "AND d.status = 'current' " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.createdDate DESC")
    List<DocumentReferenceEntity> findLatestByType(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("typeCode") String typeCode,
            Pageable pageable);

    /**
     * Full-text search in description
     */
    @Query("SELECT d FROM DocumentReferenceEntity d WHERE d.tenantId = :tenantId " +
           "AND d.patientId = :patientId " +
           "AND LOWER(d.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.createdDate DESC")
    List<DocumentReferenceEntity> searchByDescription(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("searchTerm") String searchTerm);

    /**
     * Find all document references for tenant (paginated)
     */
    Page<DocumentReferenceEntity> findByTenantIdAndDeletedAtIsNullOrderByLastModifiedAtDesc(
            String tenantId, Pageable pageable);
}
