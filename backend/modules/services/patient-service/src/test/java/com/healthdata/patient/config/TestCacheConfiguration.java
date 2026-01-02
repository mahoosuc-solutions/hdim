package com.healthdata.patient.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test Cache Configuration for Patient Service Integration Tests
 *
 * Provides an in-memory cache implementation for testing:
 * - Uses ConcurrentMapCacheManager for fast, in-memory caching
 * - No Redis dependency required for tests
 * - Caches are cleared between tests via @Transactional rollback
 */
@TestConfiguration
public class TestCacheConfiguration {

    @Bean
    @Primary
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager(
                // PatientAggregationService caches
                "patientHealthRecord",
                "patientAllergies",
                "patientImmunizations",
                "patientMedications",
                "patientConditions",
                "patientProcedures",
                "patientVitals",
                "patientLabs",
                "patientEncounters",
                "patientCarePlans",
                "patientGoals",
                // PatientHealthStatusService caches
                "patientHealthStatus",
                "patientMedicationSummary",
                "patientAllergySummary",
                "patientConditionSummary",
                "patientImmunizationSummary",
                // PatientTimelineService caches
                "patientTimeline",
                // Repository/Entity caches
                "patientDemographics",
                "patientInsurance",
                "patientRiskScores"
        );
    }
}
