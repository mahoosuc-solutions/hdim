package com.healthdata.sales.repository;

import com.healthdata.sales.entity.Opportunity;
import com.healthdata.sales.entity.OpportunityStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    Page<Opportunity> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Opportunity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Opportunity> findByZohoOpportunityId(String zohoOpportunityId);

    Page<Opportunity> findByTenantIdAndAccountId(UUID tenantId, UUID accountId, Pageable pageable);

    List<Opportunity> findByAccountId(UUID accountId);

    Page<Opportunity> findByTenantIdAndStage(UUID tenantId, OpportunityStage stage, Pageable pageable);

    @Query("SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId AND o.ownerUserId = :userId")
    Page<Opportunity> findByTenantIdAndOwner(@Param("tenantId") UUID tenantId,
                                              @Param("userId") UUID userId,
                                              Pageable pageable);

    @Query("SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId " +
           "AND o.expectedCloseDate BETWEEN :startDate AND :endDate")
    Page<Opportunity> findByExpectedCloseDateRange(@Param("tenantId") UUID tenantId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    Pageable pageable);

    @Query("SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId " +
           "AND o.stage NOT IN (com.healthdata.sales.entity.OpportunityStage.CLOSED_WON, " +
           "com.healthdata.sales.entity.OpportunityStage.CLOSED_LOST)")
    Page<Opportunity> findOpenOpportunities(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId " +
           "AND o.stage = com.healthdata.sales.entity.OpportunityStage.CLOSED_WON")
    Page<Opportunity> findWonOpportunities(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId " +
           "AND o.stage = com.healthdata.sales.entity.OpportunityStage.CLOSED_LOST")
    Page<Opportunity> findLostOpportunities(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT SUM(o.amount) FROM Opportunity o WHERE o.tenantId = :tenantId " +
           "AND o.stage NOT IN (com.healthdata.sales.entity.OpportunityStage.CLOSED_WON, " +
           "com.healthdata.sales.entity.OpportunityStage.CLOSED_LOST)")
    BigDecimal sumOpenPipelineValue(@Param("tenantId") UUID tenantId);

    @Query("SELECT SUM(o.amount * o.probability / 100) FROM Opportunity o WHERE o.tenantId = :tenantId " +
           "AND o.stage NOT IN (com.healthdata.sales.entity.OpportunityStage.CLOSED_WON, " +
           "com.healthdata.sales.entity.OpportunityStage.CLOSED_LOST)")
    BigDecimal sumWeightedPipelineValue(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(o) FROM Opportunity o WHERE o.tenantId = :tenantId AND o.stage = :stage")
    Long countByTenantIdAndStage(@Param("tenantId") UUID tenantId,
                                  @Param("stage") OpportunityStage stage);

    @Query("SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId AND o.zohoOpportunityId IS NULL")
    List<Opportunity> findUnsyncedOpportunities(@Param("tenantId") UUID tenantId);

    @Query("SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId " +
           "AND LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Opportunity> searchByName(@Param("tenantId") UUID tenantId,
                                    @Param("search") String search,
                                    Pageable pageable);
}
