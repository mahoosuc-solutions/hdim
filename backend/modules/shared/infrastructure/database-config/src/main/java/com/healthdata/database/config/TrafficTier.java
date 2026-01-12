package com.healthdata.database.config;

/**
 * Traffic tier classification for database connection pool sizing.
 *
 * Based on Phase 3 HikariCP standardization analysis (January 2026):
 * - HIGH: Core clinical data services (FHIR, CQL, Quality Measures)
 * - MEDIUM: Patient data, analytics, gateway services
 * - LOW: Supporting services (notifications, consent, documentation)
 *
 * Each tier provides sensible defaults for connection pool sizing based on
 * observed traffic patterns and performance analysis.
 */
public enum TrafficTier {

    /**
     * HIGH traffic services: 50 connections, 10 min idle.
     *
     * Used by services handling high-frequency clinical data operations:
     * - fhir-service: FHIR R4 resource operations
     * - quality-measure-service: HEDIS measure evaluations
     * - cql-engine-service: CQL expression evaluation
     *
     * These services typically handle hundreds of concurrent requests and
     * require larger connection pools to prevent queuing.
     */
    HIGH(50, 10),

    /**
     * MEDIUM traffic services: 20 connections, 5 min idle.
     *
     * Used by services with moderate traffic patterns:
     * - patient-service: Patient demographics and records
     * - care-gap-service: Care gap identification
     * - analytics-service: Quality reporting and analytics
     * - gateway-service: API gateway routing
     * - And 14 other services
     *
     * These services handle steady traffic but don't require the large
     * connection pools of HIGH tier services.
     */
    MEDIUM(20, 5),

    /**
     * LOW traffic services: 10 connections, 5 min idle.
     *
     * Used by services with lower traffic or batch-oriented operations:
     * - notification-service: Email/SMS notifications
     * - consent-service: Patient consent management
     * - documentation-service: Clinical documentation
     * - And 10 other services
     *
     * These services can operate efficiently with smaller connection pools,
     * reducing resource consumption while maintaining performance.
     */
    LOW(10, 5);

    private final int poolSize;
    private final int minIdle;

    TrafficTier(int poolSize, int minIdle) {
        this.poolSize = poolSize;
        this.minIdle = minIdle;
    }

    /**
     * Get the maximum pool size for this traffic tier.
     *
     * @return maximum number of connections in the pool
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Get the minimum idle connections for this traffic tier.
     *
     * @return minimum number of idle connections to maintain
     */
    public int getMinIdle() {
        return minIdle;
    }
}
