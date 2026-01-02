package com.healthdata.approval.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing the pool of available reviewers.
 *
 * Features:
 * - Tracks available reviewers by tenant and role
 * - Implements round-robin selection for load balancing
 * - Supports online/offline status
 * - Handles workload balancing based on current assignment count
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewerPoolService {

    private final RedisTemplate<String, String> redisTemplate;

    // Fallback in-memory storage if Redis is unavailable
    private final Map<String, Set<String>> fallbackReviewerPool = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    private static final String REVIEWER_SET_KEY = "hdim:approval:reviewers:%s:%s"; // tenant:role
    private static final String ROUND_ROBIN_KEY = "hdim:approval:roundrobin:%s:%s"; // tenant:role
    private static final String REVIEWER_STATUS_KEY = "hdim:approval:reviewer:status:%s"; // userId
    private static final Duration STATUS_TTL = Duration.ofMinutes(30);

    /**
     * Register a reviewer as available for a specific role.
     */
    public void registerReviewer(String tenantId, String role, String userId) {
        String key = String.format(REVIEWER_SET_KEY, tenantId, role);

        try {
            redisTemplate.opsForSet().add(key, userId);
            updateReviewerStatus(userId, true);
            log.info("Registered reviewer {} for role {} in tenant {}", userId, role, tenantId);
        } catch (Exception e) {
            // Fallback to in-memory
            log.warn("Redis unavailable, using in-memory fallback for reviewer registration");
            fallbackReviewerPool.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(userId);
        }
    }

    /**
     * Unregister a reviewer from a specific role.
     */
    public void unregisterReviewer(String tenantId, String role, String userId) {
        String key = String.format(REVIEWER_SET_KEY, tenantId, role);

        try {
            redisTemplate.opsForSet().remove(key, userId);
            log.info("Unregistered reviewer {} from role {} in tenant {}", userId, role, tenantId);
        } catch (Exception e) {
            Set<String> reviewers = fallbackReviewerPool.get(key);
            if (reviewers != null) {
                reviewers.remove(userId);
            }
        }
    }

    /**
     * Get all available reviewers for a tenant and role.
     */
    public List<String> getAvailableReviewers(String tenantId, String role) {
        String key = String.format(REVIEWER_SET_KEY, tenantId, role);

        try {
            Set<String> reviewers = redisTemplate.opsForSet().members(key);
            if (reviewers == null || reviewers.isEmpty()) {
                return getDefaultReviewers(tenantId, role);
            }

            // Filter by online status
            List<String> onlineReviewers = reviewers.stream()
                .filter(this::isReviewerOnline)
                .toList();

            return onlineReviewers.isEmpty() ? new ArrayList<>(reviewers) : onlineReviewers;
        } catch (Exception e) {
            // Fallback to in-memory
            Set<String> fallback = fallbackReviewerPool.get(key);
            if (fallback != null && !fallback.isEmpty()) {
                return new ArrayList<>(fallback);
            }
            return getDefaultReviewers(tenantId, role);
        }
    }

    /**
     * Select the next reviewer using round-robin.
     */
    public String selectNextReviewer(String tenantId, String role, List<String> eligibleReviewers) {
        if (eligibleReviewers.isEmpty()) {
            throw new IllegalStateException("No eligible reviewers available");
        }

        if (eligibleReviewers.size() == 1) {
            return eligibleReviewers.get(0);
        }

        String key = String.format(ROUND_ROBIN_KEY, tenantId, role);
        int index;

        try {
            Long counter = redisTemplate.opsForValue().increment(key);
            index = (counter != null ? counter.intValue() : 0) % eligibleReviewers.size();
        } catch (Exception e) {
            // Fallback to in-memory counter
            AtomicInteger counter = roundRobinCounters.computeIfAbsent(key, k -> new AtomicInteger(0));
            index = counter.getAndIncrement() % eligibleReviewers.size();
        }

        return eligibleReviewers.get(Math.abs(index));
    }

    /**
     * Update a reviewer's online status (heartbeat).
     */
    public void updateReviewerStatus(String userId, boolean online) {
        String key = String.format(REVIEWER_STATUS_KEY, userId);

        try {
            if (online) {
                redisTemplate.opsForValue().set(key, "online", STATUS_TTL);
            } else {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.debug("Could not update reviewer status in Redis: {}", e.getMessage());
        }
    }

    /**
     * Check if a reviewer is currently online.
     */
    public boolean isReviewerOnline(String userId) {
        String key = String.format(REVIEWER_STATUS_KEY, userId);

        try {
            String status = redisTemplate.opsForValue().get(key);
            return "online".equals(status);
        } catch (Exception e) {
            // If Redis is unavailable, assume online
            return true;
        }
    }

    /**
     * Get count of currently assigned requests for a reviewer.
     */
    public int getAssignmentCount(String userId) {
        // This would typically query the database
        // For now, return 0 as placeholder
        return 0;
    }

    /**
     * Get default reviewers when none are explicitly registered.
     * This provides a fallback based on known roles in the system.
     */
    private List<String> getDefaultReviewers(String tenantId, String role) {
        // Return placeholder defaults - in production, this would query a user service
        // or return empty to force explicit registration
        log.warn("No reviewers registered for role {} in tenant {}, using defaults", role, tenantId);

        return switch (role) {
            case "CLINICAL_REVIEWER" -> List.of("default-clinical-reviewer");
            case "CLINICAL_SUPERVISOR" -> List.of("default-clinical-supervisor");
            case "CLINICAL_DIRECTOR" -> List.of("default-clinical-director");
            case "TECHNICAL_REVIEWER" -> List.of("default-technical-reviewer");
            case "TECHNICAL_LEAD" -> List.of("default-technical-lead");
            case "ADMIN" -> List.of("admin");
            default -> Collections.emptyList();
        };
    }

    /**
     * Bulk register reviewers (useful for initialization).
     */
    public void bulkRegisterReviewers(String tenantId, Map<String, List<String>> roleToReviewers) {
        roleToReviewers.forEach((role, reviewers) -> {
            reviewers.forEach(reviewer -> registerReviewer(tenantId, role, reviewer));
        });
        log.info("Bulk registered {} roles with reviewers for tenant {}", roleToReviewers.size(), tenantId);
    }

    /**
     * Get all registered reviewers for a tenant (all roles).
     */
    public Map<String, Set<String>> getAllReviewers(String tenantId) {
        Map<String, Set<String>> result = new HashMap<>();
        List<String> roles = List.of(
            "CLINICAL_REVIEWER", "CLINICAL_SUPERVISOR", "CLINICAL_DIRECTOR",
            "TECHNICAL_REVIEWER", "TECHNICAL_LEAD", "ADMIN"
        );

        for (String role : roles) {
            List<String> reviewers = getAvailableReviewers(tenantId, role);
            if (!reviewers.isEmpty()) {
                result.put(role, new HashSet<>(reviewers));
            }
        }

        return result;
    }
}
