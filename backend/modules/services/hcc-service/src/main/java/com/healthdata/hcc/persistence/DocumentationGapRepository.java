package com.healthdata.hcc.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentationGapRepository extends JpaRepository<DocumentationGapEntity, UUID> {

    Optional<DocumentationGapEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<DocumentationGapEntity> findByTenantIdAndPatientIdAndProfileYear(
        String tenantId, UUID patientId, Integer profileYear);

    Page<DocumentationGapEntity> findByTenantIdAndStatus(
        String tenantId, DocumentationGapEntity.GapStatus status, Pageable pageable);

    @Query("SELECT g FROM DocumentationGapEntity g WHERE g.tenantId = :tenantId " +
           "AND g.profileYear = :year AND g.status = 'OPEN' " +
           "ORDER BY g.rafImpactBlended DESC")
    List<DocumentationGapEntity> findHighImpactOpenGaps(
        @Param("tenantId") String tenantId,
        @Param("year") Integer year);

    @Query("SELECT g FROM DocumentationGapEntity g WHERE g.tenantId = :tenantId " +
           "AND g.profileYear = :year AND g.status = 'OPEN' " +
           "AND g.rafImpactBlended >= :minImpact " +
           "ORDER BY g.rafImpactBlended DESC")
    List<DocumentationGapEntity> findHighValueGaps(
        @Param("tenantId") String tenantId,
        @Param("year") Integer year,
        @Param("minImpact") BigDecimal minImpact);

    @Query("SELECT COUNT(g) FROM DocumentationGapEntity g WHERE g.tenantId = :tenantId " +
           "AND g.patientId = :patientId AND g.profileYear = :year AND g.status = 'OPEN'")
    long countOpenGapsByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("year") Integer year);
}
