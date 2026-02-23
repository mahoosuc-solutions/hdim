package com.healthdata.fhir.config;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that uses in-memory caching to avoid Redis connection issues in tests.
 * Uses SimpleCacheManager with ConcurrentMapCache and provides a CacheErrorHandler that ignores all cache errors.
 *
 * Cache names must match exactly what the service code uses (CACHE_NAME constants and @Cacheable values).
 */
@TestConfiguration
@EnableCaching
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class})
public class TestCacheConfiguration implements CachingConfigurer {

    @Bean
    @Primary
    @Override
    public CacheManager cacheManager() {
        // Use SimpleCacheManager with in-memory caches for testing
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            // AllergyIntolerance caches
            new ConcurrentMapCache("allergyIntolerances"),
            new ConcurrentMapCache("patientAllergies"),
            // Immunization caches
            new ConcurrentMapCache("immunizations"),
            new ConcurrentMapCache("patientImmunizations"),
            // Encounter caches
            new ConcurrentMapCache("encounters"),
            new ConcurrentMapCache("encountersByPatient"),
            // Procedure caches
            new ConcurrentMapCache("procedures"),
            new ConcurrentMapCache("proceduresByPatient"),
            // Goal caches — GoalService uses CACHE_NAME = "fhir-goals"
            new ConcurrentMapCache("fhir-goals"),
            new ConcurrentMapCache("goals"),
            // CarePlan caches — CarePlanService uses CACHE_NAME = "fhir-care-plans"
            new ConcurrentMapCache("fhir-care-plans"),
            new ConcurrentMapCache("fhir-careplans"),
            new ConcurrentMapCache("careplans"),
            // Coverage caches — CoverageService uses CACHE_NAME = "fhir-coverages"
            new ConcurrentMapCache("fhir-coverages"),
            new ConcurrentMapCache("coverages"),
            // DiagnosticReport caches — DiagnosticReportService uses CACHE_NAME = "fhir-diagnostic-reports"
            new ConcurrentMapCache("fhir-diagnostic-reports"),
            new ConcurrentMapCache("fhir-diagnosticreports"),
            new ConcurrentMapCache("diagnosticreports"),
            // DocumentReference caches — DocumentReferenceService uses CACHE_NAME = "fhir-document-references"
            new ConcurrentMapCache("fhir-document-references"),
            new ConcurrentMapCache("fhir-documentreferences"),
            new ConcurrentMapCache("documentreferences"),
            // MedicationRequest caches — MedicationRequestService uses CACHE_NAME = "fhir-medication-requests"
            new ConcurrentMapCache("fhir-medication-requests"),
            new ConcurrentMapCache("medicationrequests"),
            // MedicationAdministration caches — MedicationAdministrationService uses CACHE_NAME = "fhir-medication-administrations"
            new ConcurrentMapCache("fhir-medication-administrations"),
            new ConcurrentMapCache("medicationadministrations"),
            // Patient caches — PatientService uses CACHE_NAME = "fhir-patients"
            new ConcurrentMapCache("patients"),
            new ConcurrentMapCache("fhir-patients"),
            // Observation caches — ObservationService uses CACHE_NAME = "fhir-observations"
            new ConcurrentMapCache("observations"),
            new ConcurrentMapCache("fhir-observations"),
            // Condition caches — ConditionService uses CACHE_NAME = "fhir-conditions"
            new ConcurrentMapCache("conditions"),
            new ConcurrentMapCache("fhir-conditions"),
            // Task caches
            new ConcurrentMapCache("tasks"),
            new ConcurrentMapCache("tasksByPatient"),
            // Appointment caches
            new ConcurrentMapCache("appointments"),
            new ConcurrentMapCache("appointmentsByPatient"),
            // Practitioner caches — PractitionerService uses CACHE_NAME = "fhir-practitioners"
            new ConcurrentMapCache("fhir-practitioners"),
            new ConcurrentMapCache("practitioners"),
            // PractitionerRole caches — PractitionerRoleService uses CACHE_NAME = "fhir-practitioner-roles"
            new ConcurrentMapCache("fhir-practitioner-roles"),
            new ConcurrentMapCache("practitionerroles"),
            // Organization caches — OrganizationService uses CACHE_NAME = "fhir-organizations"
            new ConcurrentMapCache("fhir-organizations"),
            new ConcurrentMapCache("organizations"),
            // General FHIR resources cache
            new ConcurrentMapCache("fhirResources")
        ));
        return cacheManager;
    }

    @Override
    public CacheResolver cacheResolver() {
        return null; // Use default
    }

    @Override
    public KeyGenerator keyGenerator() {
        return null; // Use default
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        // Return an error handler that silently ignores all cache errors
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // Ignore error
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                // Ignore error
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // Ignore error
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                // Ignore error
            }
        };
    }
}
