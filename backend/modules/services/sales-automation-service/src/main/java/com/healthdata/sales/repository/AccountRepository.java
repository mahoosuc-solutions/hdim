package com.healthdata.sales.repository;

import com.healthdata.sales.entity.Account;
import com.healthdata.sales.entity.AccountStage;
import com.healthdata.sales.entity.OrganizationType;
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
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Page<Account> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Account> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Account> findByZohoAccountId(String zohoAccountId);

    Page<Account> findByTenantIdAndStage(UUID tenantId, AccountStage stage, Pageable pageable);

    Page<Account> findByTenantIdAndOrganizationType(UUID tenantId, OrganizationType type, Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.ownerUserId = :userId")
    Page<Account> findByTenantIdAndOwner(@Param("tenantId") UUID tenantId,
                                          @Param("userId") UUID userId,
                                          Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId " +
           "AND LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Account> searchByName(@Param("tenantId") UUID tenantId,
                                @Param("search") String search,
                                Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId " +
           "AND a.patientCount >= :minPatients")
    Page<Account> findByMinPatientCount(@Param("tenantId") UUID tenantId,
                                         @Param("minPatients") Integer minPatients,
                                         Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.state = :state")
    Page<Account> findByState(@Param("tenantId") UUID tenantId,
                               @Param("state") String state,
                               Pageable pageable);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.tenantId = :tenantId AND a.stage = :stage")
    Long countByTenantIdAndStage(@Param("tenantId") UUID tenantId,
                                  @Param("stage") AccountStage stage);

    @Query("SELECT a FROM Account a WHERE a.tenantId = :tenantId AND a.zohoAccountId IS NULL")
    List<Account> findUnsyncedAccounts(@Param("tenantId") UUID tenantId);

    boolean existsByNameAndTenantId(String name, UUID tenantId);
}
