package com.healthdata.sales.repository;

import com.healthdata.sales.entity.Contact;
import com.healthdata.sales.entity.ContactType;
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
public interface ContactRepository extends JpaRepository<Contact, UUID> {

    Page<Contact> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Contact> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Contact> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<Contact> findByZohoContactId(String zohoContactId);

    Page<Contact> findByTenantIdAndAccountId(UUID tenantId, UUID accountId, Pageable pageable);

    List<Contact> findByAccountId(UUID accountId);

    @Query("SELECT c FROM Contact c WHERE c.accountId = :accountId AND c.primary = true")
    Optional<Contact> findPrimaryContactByAccountId(@Param("accountId") UUID accountId);

    Page<Contact> findByTenantIdAndContactType(UUID tenantId, ContactType type, Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.ownerUserId = :userId")
    Page<Contact> findByTenantIdAndOwner(@Param("tenantId") UUID tenantId,
                                          @Param("userId") UUID userId,
                                          Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId " +
           "AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Contact> searchContacts(@Param("tenantId") UUID tenantId,
                                  @Param("search") String search,
                                  Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId " +
           "AND c.doNotCall = false AND c.doNotEmail = false")
    Page<Contact> findContactableContacts(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Contact c WHERE c.tenantId = :tenantId AND c.accountId = :accountId")
    Long countByAccountId(@Param("tenantId") UUID tenantId,
                           @Param("accountId") UUID accountId);

    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.zohoContactId IS NULL")
    List<Contact> findUnsyncedContacts(@Param("tenantId") UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);
}
