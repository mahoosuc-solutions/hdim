package com.healthdata.cql.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-based cache service for compiled measure templates
 *
 * Provides high-performance caching of parsed and compiled measure templates
 * to avoid repeated parsing of CQL/ELM content.
 *
 * Thread-safe and optimized for concurrent access.
 */
@Service
public class TemplateCacheService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCacheService.class);
    private static final String CACHE_PREFIX = "measure:template:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public TemplateCacheService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Get a cached template by ID
     *
     * @param templateId Template identifier
     * @return Cached template or null if not found
     */
    public MeasureTemplate getTemplate(UUID templateId) {
        String key = getCacheKey(templateId);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                logger.debug("Cache HIT for template {}", templateId);
                return objectMapper.readValue(json, MeasureTemplate.class);
            }
            logger.debug("Cache MISS for template {}", templateId);
            return null;
        } catch (Exception e) {
            logger.error("Error reading template from cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get a cached template by measure ID and tenant
     *
     * @param measureId Measure identifier (e.g., "HEDIS-CDC")
     * @param tenantId Tenant identifier
     * @return Cached template or null if not found
     */
    public MeasureTemplate getTemplateByMeasureId(String measureId, String tenantId) {
        String key = getCacheKeyByMeasure(measureId, tenantId);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                logger.debug("Cache HIT for measure {} / tenant {}", measureId, tenantId);
                return objectMapper.readValue(json, MeasureTemplate.class);
            }
            logger.debug("Cache MISS for measure {} / tenant {}", measureId, tenantId);
            return null;
        } catch (Exception e) {
            logger.error("Error reading template from cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Cache a template
     *
     * @param template Template to cache
     */
    public void putTemplate(MeasureTemplate template) {
        putTemplate(template, DEFAULT_TTL);
    }

    /**
     * Cache a template with custom TTL
     *
     * @param template Template to cache
     * @param ttl Time to live
     */
    public void putTemplate(MeasureTemplate template, Duration ttl) {
        if (template == null || template.getTemplateId() == null) {
            logger.warn("Cannot cache null template or template with null ID");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(template);

            // Cache by template ID
            String key = getCacheKey(template.getTemplateId());
            redisTemplate.opsForValue().set(key, json, ttl);

            // Also cache by measure ID + tenant for faster lookup
            if (template.getMeasureId() != null && template.getTenantId() != null) {
                String measureKey = getCacheKeyByMeasure(template.getMeasureId(), template.getTenantId());
                redisTemplate.opsForValue().set(measureKey, json, ttl);
            }

            logger.debug("Cached template {} (measure={}, tenant={})",
                    template.getTemplateId(),
                    template.getMeasureId(),
                    template.getTenantId());
        } catch (JsonProcessingException e) {
            logger.error("Error serializing template for cache: {}", e.getMessage());
        }
    }

    /**
     * Invalidate a cached template
     *
     * @param templateId Template identifier
     */
    public void invalidate(UUID templateId) {
        String key = getCacheKey(templateId);
        Boolean deleted = redisTemplate.delete(key);
        logger.debug("Invalidated template {}: {}", templateId, deleted);
    }

    /**
     * Invalidate a cached template by measure ID and tenant
     *
     * @param measureId Measure identifier
     * @param tenantId Tenant identifier
     */
    public void invalidateByMeasureId(String measureId, String tenantId) {
        String key = getCacheKeyByMeasure(measureId, tenantId);
        Boolean deleted = redisTemplate.delete(key);
        logger.debug("Invalidated measure {} / tenant {}: {}", measureId, tenantId, deleted);
    }

    /**
     * Clear all cached templates
     * WARNING: This clears ALL measure templates from cache
     */
    public void clearAll() {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                logger.info("Cleared {} templates from cache", deleted);
            }
        } catch (Exception e) {
            logger.error("Error clearing template cache: {}", e.getMessage());
        }
    }

    /**
     * Get cache statistics
     *
     * @return Count of cached templates
     */
    public long getCacheSize() {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            logger.error("Error getting cache size: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Generate cache key for template ID
     */
    private String getCacheKey(UUID templateId) {
        return CACHE_PREFIX + templateId;
    }

    /**
     * Generate cache key for measure ID + tenant
     */
    private String getCacheKeyByMeasure(String measureId, String tenantId) {
        return CACHE_PREFIX + "measure:" + tenantId + ":" + measureId;
    }
}
