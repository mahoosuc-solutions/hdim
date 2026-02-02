package com.healthdata.investor.repository;

import com.healthdata.investor.entity.InvestorTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for InvestorTask entity.
 */
@Repository
public interface InvestorTaskRepository extends JpaRepository<InvestorTask, UUID> {

    List<InvestorTask> findByStatusOrderBySortOrderAsc(String status);

    List<InvestorTask> findByCategoryOrderBySortOrderAsc(String category);

    List<InvestorTask> findByWeekOrderBySortOrderAsc(Integer week);

    List<InvestorTask> findAllByOrderBySortOrderAsc();

    @Query("SELECT t FROM InvestorTask t ORDER BY t.week ASC, t.sortOrder ASC")
    List<InvestorTask> findAllOrderByWeekAndSortOrder();

    @Query("SELECT COUNT(t) FROM InvestorTask t WHERE t.status = :status")
    long countByStatus(String status);

    @Query("SELECT t.category, COUNT(t) FROM InvestorTask t GROUP BY t.category")
    List<Object[]> countByCategory();

    @Query("SELECT t.status, COUNT(t) FROM InvestorTask t GROUP BY t.status")
    List<Object[]> countByStatusGrouped();

    List<InvestorTask> findBySubjectContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String subject, String description);
}
