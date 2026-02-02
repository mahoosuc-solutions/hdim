package com.healthdata.investor.repository;

import com.healthdata.investor.entity.InvestorContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for InvestorContact entity.
 */
@Repository
public interface InvestorContactRepository extends JpaRepository<InvestorContact, UUID> {

    List<InvestorContact> findByCategoryOrderByNameAsc(String category);

    List<InvestorContact> findByStatusOrderByNameAsc(String status);

    List<InvestorContact> findByTierOrderByNameAsc(String tier);

    List<InvestorContact> findAllByOrderByTierAscNameAsc();

    Optional<InvestorContact> findByLinkedInProfileId(String linkedInProfileId);

    @Query("SELECT c FROM InvestorContact c WHERE c.linkedInProfileId IS NOT NULL")
    List<InvestorContact> findAllWithLinkedInProfile();

    @Query("SELECT c.category, COUNT(c) FROM InvestorContact c GROUP BY c.category")
    List<Object[]> countByCategory();

    @Query("SELECT c.status, COUNT(c) FROM InvestorContact c GROUP BY c.status")
    List<Object[]> countByStatus();

    @Query("SELECT c.tier, COUNT(c) FROM InvestorContact c GROUP BY c.tier")
    List<Object[]> countByTier();

    List<InvestorContact> findByNameContainingIgnoreCaseOrOrganizationContainingIgnoreCase(
            String name, String organization);

    @Query("SELECT c FROM InvestorContact c WHERE c.nextFollowUp IS NOT NULL AND c.nextFollowUp <= CURRENT_TIMESTAMP ORDER BY c.nextFollowUp ASC")
    List<InvestorContact> findContactsNeedingFollowUp();
}
