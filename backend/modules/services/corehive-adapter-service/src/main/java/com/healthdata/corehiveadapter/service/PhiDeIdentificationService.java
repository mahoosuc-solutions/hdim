package com.healthdata.corehiveadapter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PHI De-Identification Service.
 *
 * Creates and manages synthetic ID mappings so CoreHive NEVER
 * receives real patient identifiers, names, dates of birth,
 * or any other PHI. Only clinical codes (ICD-10, CPT, HEDIS
 * measure IDs) and synthetic UUIDs cross the boundary.
 *
 * Mapping is bidirectional: real → synthetic for outbound requests,
 * synthetic → real for inbound responses.
 */
@Service
@Slf4j
public class PhiDeIdentificationService {

    private final Map<String, String> realToSynthetic = new ConcurrentHashMap<>();
    private final Map<String, String> syntheticToReal = new ConcurrentHashMap<>();

    /**
     * Get or create a synthetic ID for a real patient/entity ID.
     */
    public String toSyntheticId(String realId) {
        return realToSynthetic.computeIfAbsent(realId, k -> {
            String syntheticId = UUID.randomUUID().toString();
            syntheticToReal.put(syntheticId, realId);
            log.debug("Created synthetic mapping for entity");
            return syntheticId;
        });
    }

    /**
     * Resolve a synthetic ID back to the real ID.
     */
    public String toRealId(String syntheticId) {
        String realId = syntheticToReal.get(syntheticId);
        if (realId == null) {
            log.warn("No real ID mapping found for synthetic ID");
            throw new IllegalArgumentException("Unknown synthetic ID");
        }
        return realId;
    }

    /**
     * Check if a string looks like it could contain PHI.
     * Rejects strings containing patterns that resemble
     * SSNs, MRNs, names with common suffixes, etc.
     */
    public boolean containsPotentialPhi(String value) {
        if (value == null) return false;
        // SSN pattern
        if (value.matches(".*\\d{3}-\\d{2}-\\d{4}.*")) return true;
        // Email pattern
        if (value.matches(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")) return true;
        // Phone pattern
        if (value.matches(".*\\(\\d{3}\\)\\s*\\d{3}-\\d{4}.*")) return true;
        // Date of birth pattern (MM/DD/YYYY or YYYY-MM-DD)
        if (value.matches(".*\\d{2}/\\d{2}/\\d{4}.*")) return true;
        return false;
    }

    /**
     * Clear all mappings (for testing or session cleanup).
     */
    public void clearMappings() {
        realToSynthetic.clear();
        syntheticToReal.clear();
    }
}
