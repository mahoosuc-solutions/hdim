package com.healthdata.authentication.service;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.repository.UserRepository;
import com.healthdata.cache.CacheEvictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * HIPAA-Compliant Logout Service
 *
 * ⚠️ CRITICAL SECURITY SERVICE - DO NOT MODIFY WITHOUT COMPLIANCE REVIEW ⚠️
 *
 * Handles user logout with automatic PHI cache eviction to ensure compliance
 * with HIPAA data minimization requirements.
 *
 * HIPAA Regulation: 45 CFR 164.312(a)(2)(i) - Access Controls
 *
 * This service ensures that:
 * 1. User sessions are properly terminated
 * 2. All PHI caches for the user's tenants are cleared
 * 3. Cache eviction is logged for audit purposes
 *
 * For complete documentation, see: /backend/HIPAA-CACHE-COMPLIANCE.md
 *
 * @author Claude Code
 * @since 2025-11-14
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogoutService {

    private final UserRepository userRepository;
    private final CacheEvictionService cacheEvictionService;

    /**
     * Perform HIPAA-compliant logout with cache eviction
     *
     * ⚠️ CRITICAL: This method MUST be called on user logout to ensure PHI is not retained
     *
     * Steps performed:
     * 1. Load user details to get tenant associations
     * 2. Evict all PHI caches for each of the user's tenants
     * 3. Log cache eviction for audit trail
     *
     * @param username The username of the user logging out
     */
    public void performLogout(String username) {
        if (username == null || username.isBlank()) {
            log.warn("Attempted logout with null or blank username");
            return;
        }

        log.info("Performing HIPAA-compliant logout for user: {}", username);

        try {
            // Load user to get tenant associations
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                log.warn("User not found during logout: {}", username);
                // Still try to evict caches even if user not found
                // This handles edge cases where user was deleted but session still active
                evictAllPhiCaches();
                return;
            }

            User user = userOpt.get();
            Set<String> tenantIds = user.getTenantIds();

            if (tenantIds == null || tenantIds.isEmpty()) {
                log.warn("User {} has no tenant associations, clearing all PHI caches", username);
                evictAllPhiCaches();
                return;
            }

            // Evict caches for each tenant the user has access to
            int cachesCleared = 0;
            for (String tenantId : tenantIds) {
                log.debug("Clearing PHI caches for tenant: {}", tenantId);
                cacheEvictionService.evictTenantCaches(tenantId);
                cachesCleared++;
            }

            log.info("Logout complete for user: {} - Cleared caches for {} tenants",
                    username, cachesCleared);

        } catch (Exception e) {
            log.error("Error during cache eviction for user: {}", username, e);
            // On error, try to clear all PHI caches as a safety measure
            log.warn("Attempting fallback: clearing all PHI caches due to logout error");
            try {
                evictAllPhiCaches();
            } catch (Exception fallbackError) {
                log.error("Fallback cache eviction also failed", fallbackError);
            }
        }
    }

    /**
     * Perform logout with cache eviction by email
     *
     * @param email The email of the user logging out
     */
    public void performLogoutByEmail(String email) {
        if (email == null || email.isBlank()) {
            log.warn("Attempted logout with null or blank email");
            return;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            performLogout(userOpt.get().getUsername());
        } else {
            log.warn("User not found by email during logout: {}", email);
            evictAllPhiCaches();
        }
    }

    /**
     * Emergency cache clear - evicts all PHI caches
     *
     * ⚠️ USE WITH CAUTION: This clears PHI caches for all tenants
     *
     * Use cases:
     * - User not found during logout (safety measure)
     * - Error during tenant-specific eviction
     * - Security incident response
     */
    private void evictAllPhiCaches() {
        log.warn("Evicting all PHI caches (emergency measure)");
        cacheEvictionService.evictAllPhiCaches();
    }

    /**
     * Check if cache eviction is properly configured
     *
     * @return true if cache eviction service is available
     */
    public boolean isCacheEvictionAvailable() {
        try {
            return cacheEvictionService.isCacheManagerAvailable();
        } catch (Exception e) {
            log.error("Error checking cache eviction availability", e);
            return false;
        }
    }
}
