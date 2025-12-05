package com.healthdata.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Stub CacheEvictionService for Gateway
 * Gateway doesn't cache PHI data, so this is a no-op implementation
 * 
 * This stub satisfies the dependency in LogoutService from the authentication module
 * while avoiding the circular dependency issues with the full cache module.
 */
@Service
@Slf4j
public class CacheEvictionService {

    public void evictPhiCachesForTenants(Set<String> tenantIds) {
        log.info("Gateway: Cache eviction skipped (no PHI cached), tenants: {}", tenantIds);
    }

    public void evictAllPhiCaches() {
        log.info("Gateway: Cache eviction skipped (no PHI cached)");
    }
}
