package com.healthdata.investor.repository;

import com.healthdata.investor.entity.OutreachActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for OutreachActivity entity.
 */
@Repository
public interface OutreachActivityRepository extends JpaRepository<OutreachActivity, UUID> {

    List<OutreachActivity> findByContactIdOrderByActivityDateDesc(UUID contactId);

    List<OutreachActivity> findByActivityTypeOrderByActivityDateDesc(String activityType);

    List<OutreachActivity> findByStatusOrderByActivityDateDesc(String status);

    List<OutreachActivity> findByActivityDateBetweenOrderByActivityDateDesc(LocalDate start, LocalDate end);

    List<OutreachActivity> findAllByOrderByActivityDateDesc();

    @Query("SELECT a.activityType, COUNT(a) FROM OutreachActivity a GROUP BY a.activityType")
    List<Object[]> countByActivityType();

    @Query("SELECT a.status, COUNT(a) FROM OutreachActivity a GROUP BY a.status")
    List<Object[]> countByStatus();

    @Query("SELECT a FROM OutreachActivity a WHERE a.activityType LIKE 'linkedin_%' ORDER BY a.activityDate DESC")
    List<OutreachActivity> findLinkedInActivities();

    @Query("SELECT a FROM OutreachActivity a WHERE a.status = 'pending' AND a.scheduledTime IS NOT NULL AND a.scheduledTime <= CURRENT_TIMESTAMP ORDER BY a.scheduledTime ASC")
    List<OutreachActivity> findPendingScheduledActivities();

    long countByContactId(UUID contactId);
}
