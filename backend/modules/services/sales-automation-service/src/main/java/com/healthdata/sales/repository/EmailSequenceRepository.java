package com.healthdata.sales.repository;

import com.healthdata.sales.entity.EmailSequence;
import com.healthdata.sales.entity.SequenceType;
import com.healthdata.sales.entity.TargetType;
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
public interface EmailSequenceRepository extends JpaRepository<EmailSequence, UUID> {

    Page<EmailSequence> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<EmailSequence> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<EmailSequence> findByTenantIdAndActive(UUID tenantId, Boolean active, Pageable pageable);

    Page<EmailSequence> findByTenantIdAndSequenceType(UUID tenantId, SequenceType type, Pageable pageable);

    Page<EmailSequence> findByTenantIdAndTargetType(UUID tenantId, TargetType targetType, Pageable pageable);

    @Query("SELECT s FROM EmailSequence s WHERE s.tenantId = :tenantId AND s.active = true " +
           "AND (s.targetType = :targetType OR s.targetType = 'BOTH')")
    List<EmailSequence> findActiveSequencesForTargetType(
        @Param("tenantId") UUID tenantId,
        @Param("targetType") TargetType targetType);

    @Query("SELECT s FROM EmailSequence s WHERE s.tenantId = :tenantId " +
           "AND LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<EmailSequence> searchByName(
        @Param("tenantId") UUID tenantId,
        @Param("search") String search,
        Pageable pageable);

    @Query("SELECT COUNT(s) FROM EmailSequence s WHERE s.tenantId = :tenantId AND s.active = true")
    Long countActiveSequences(@Param("tenantId") UUID tenantId);

    boolean existsByNameAndTenantId(String name, UUID tenantId);
}
