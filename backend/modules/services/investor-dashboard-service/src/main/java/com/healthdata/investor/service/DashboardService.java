package com.healthdata.investor.service;

import com.healthdata.investor.dto.DashboardStatsDTO;
import com.healthdata.investor.repository.InvestorContactRepository;
import com.healthdata.investor.repository.InvestorTaskRepository;
import com.healthdata.investor.repository.LinkedInConnectionRepository;
import com.healthdata.investor.repository.OutreachActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for aggregating dashboard statistics.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final InvestorTaskRepository taskRepository;
    private final InvestorContactRepository contactRepository;
    private final OutreachActivityRepository activityRepository;
    private final LinkedInConnectionRepository linkedInRepository;

    public DashboardStatsDTO getStats(UUID userId) {
        // Task statistics
        long totalTasks = taskRepository.count();
        long completedTasks = taskRepository.countByStatus("completed");
        long inProgressTasks = taskRepository.countByStatus("in_progress");
        long pendingTasks = taskRepository.countByStatus("pending");
        long blockedTasks = taskRepository.countByStatus("blocked");
        double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;

        Map<String, Long> tasksByCategory = aggregateToMap(taskRepository.countByCategory());
        Map<String, Long> tasksByStatus = aggregateToMap(taskRepository.countByStatusGrouped());

        // Contact statistics
        long totalContacts = contactRepository.count();
        Map<String, Long> contactsByCategory = aggregateToMap(contactRepository.countByCategory());
        Map<String, Long> contactsByStatus = aggregateToMap(contactRepository.countByStatus());
        Map<String, Long> contactsByTier = aggregateToMap(contactRepository.countByTier());
        long contactsNeedingFollowUp = contactRepository.findContactsNeedingFollowUp().size();

        // Activity statistics
        long totalActivities = activityRepository.count();
        Map<String, Long> activitiesByType = aggregateToMap(activityRepository.countByActivityType());
        Map<String, Long> activitiesByStatus = aggregateToMap(activityRepository.countByStatus());
        long linkedInActivities = activityRepository.findLinkedInActivities().size();
        long pendingActivities = activityRepository.findPendingScheduledActivities().size();

        // LinkedIn statistics
        boolean linkedInConnected = linkedInRepository.existsByUserId(userId);
        long linkedInConnectionsSent = activitiesByType.getOrDefault("linkedin_connect", 0L);
        long linkedInMessagesSent = activitiesByType.getOrDefault("linkedin_message", 0L);

        return DashboardStatsDTO.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .pendingTasks(pendingTasks)
                .blockedTasks(blockedTasks)
                .taskCompletionRate(Math.round(completionRate * 10) / 10.0)
                .tasksByCategory(tasksByCategory)
                .tasksByWeek(calculateTasksByWeek())
                .totalContacts(totalContacts)
                .contactsByCategory(contactsByCategory)
                .contactsByStatus(contactsByStatus)
                .contactsByTier(contactsByTier)
                .contactsNeedingFollowUp(contactsNeedingFollowUp)
                .totalActivities(totalActivities)
                .activitiesByType(activitiesByType)
                .activitiesByStatus(activitiesByStatus)
                .linkedInActivities(linkedInActivities)
                .pendingActivities(pendingActivities)
                .linkedInConnected(linkedInConnected)
                .linkedInConnectionsSent(linkedInConnectionsSent)
                .linkedInMessageseSent(linkedInMessagesSent)
                .linkedInResponses(activitiesByStatus.getOrDefault("responded", 0L))
                .build();
    }

    private Map<String, Long> aggregateToMap(java.util.List<Object[]> results) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] result : results) {
            String key = (String) result[0];
            Long value = (Long) result[1];
            map.put(key, value);
        }
        return map;
    }

    private Map<String, Long> calculateTasksByWeek() {
        Map<String, Long> tasksByWeek = new HashMap<>();
        for (int week = 1; week <= 4; week++) {
            long count = taskRepository.findByWeekOrderBySortOrderAsc(week).size();
            tasksByWeek.put("Week " + week, count);
        }
        return tasksByWeek;
    }
}
