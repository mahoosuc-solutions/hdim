package com.healthdata.fhir.security.smart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores SMART launch context behind opaque launch IDs.
 * Uses in-memory storage with TTL and lazy cleanup.
 */
@Component
@Slf4j
public class SmartLaunchContextStore {

    private final ConcurrentHashMap<String, StoredLaunchContext> storage = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final long ttlSeconds;

    public SmartLaunchContextStore(
            @Value("${smart.launch-context.ttl-seconds:600}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public String storeLaunchContext(Map<String, Object> context) {
        cleanupExpiredContexts();
        String launchId = generateOpaqueLaunchId();
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        storage.put(launchId, new StoredLaunchContext(context, expiresAt));
        return launchId;
    }

    public Optional<Map<String, Object>> resolveLaunchContext(String launchId) {
        cleanupExpiredContexts();
        if (!StringUtils.hasText(launchId)) {
            return Optional.empty();
        }

        StoredLaunchContext stored = storage.get(launchId);
        if (stored == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(stored.expiresAt())) {
            storage.remove(launchId);
            return Optional.empty();
        }
        return Optional.of(stored.context());
    }

    public void cleanupExpiredContexts() {
        Instant now = Instant.now();
        storage.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    private String generateOpaqueLaunchId() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return "lc_" + HexFormat.of().formatHex(randomBytes);
    }

    record StoredLaunchContext(Map<String, Object> context, Instant expiresAt) {}
}
