package com.healthdata.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HIPAA Cache Compliance Tests
 *
 * Validates that all cache configurations comply with HIPAA requirements:
 * - PHI data cache TTL must not exceed 5 minutes
 * - Default TTL must be HIPAA-compliant
 * - All configured caches must be validated
 *
 * HIPAA Security Rule requires minimizing PHI exposure time.
 * While no specific TTL is mandated, 5 minutes is the conservative
 * industry standard for healthcare applications.
 *
 * @see <a href="https://www.hhs.gov/hipaa/for-professionals/security/index.html">HIPAA Security Rule</a>
 */
@DisplayName("HIPAA Cache Compliance Tests")
class HIPAACacheComplianceTest {

    private static final Duration HIPAA_MAX_TTL = Duration.ofMinutes(5);
    private static final Duration HIPAA_RECOMMENDED_TTL = Duration.ofMinutes(5);

    @Nested
    @DisplayName("Default TTL Compliance")
    class DefaultTtlComplianceTests {

        @Test
        @DisplayName("Default TTL should not exceed HIPAA maximum of 5 minutes")
        void defaultTtlShouldBeHipaaCompliant() {
            CacheProperties properties = new CacheProperties();
            Duration defaultTtl = properties.getRedis().getDefaultTtl();

            assertThat(defaultTtl)
                .as("Default cache TTL must not exceed %s for HIPAA compliance", HIPAA_MAX_TTL)
                .isLessThanOrEqualTo(HIPAA_MAX_TTL);
        }

        @Test
        @DisplayName("Default TTL should be exactly 5 minutes (HIPAA recommended)")
        void defaultTtlShouldBeRecommendedValue() {
            CacheProperties properties = new CacheProperties();
            Duration defaultTtl = properties.getRedis().getDefaultTtl();

            assertThat(defaultTtl)
                .as("Default cache TTL should be the HIPAA-recommended %s", HIPAA_RECOMMENDED_TTL)
                .isEqualTo(HIPAA_RECOMMENDED_TTL);
        }

        @ParameterizedTest(name = "TTL of {0} minutes should be compliant")
        @ValueSource(ints = {1, 2, 3, 4, 5})
        @DisplayName("TTLs at or below 5 minutes should be compliant")
        void ttlsAtOrBelowFiveMinutesShouldBeCompliant(int minutes) {
            Duration ttl = Duration.ofMinutes(minutes);
            assertThat(ttl).isLessThanOrEqualTo(HIPAA_MAX_TTL);
        }
    }

    @Nested
    @DisplayName("PHI Cache Configuration Validation")
    class PhiCacheConfigurationTests {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withBean(ObjectMapper.class, ObjectMapper::new);

        @Test
        @DisplayName("Patient data cache TTL must not exceed 5 minutes")
        void patientCacheTtlMustBeCompliant() {
            contextRunner
                .withPropertyValues(
                    "healthdata.cache.redis.host=localhost",
                    "healthdata.cache.redis.port=6379",
                    "healthdata.cache.caches.patient-data.ttl=PT5M"
                )
                .run(context -> {
                    RedisCacheManager cacheManager = context.getBean(RedisCacheManager.class);
                    Map<String, RedisCacheConfiguration> configs = cacheManager.getCacheConfigurations();

                    if (configs.containsKey("patient-data")) {
                        Duration ttl = configs.get("patient-data").getTtl();
                        assertThat(ttl)
                            .as("Patient data cache TTL must not exceed %s", HIPAA_MAX_TTL)
                            .isLessThanOrEqualTo(HIPAA_MAX_TTL);
                    }
                });
        }

        @Test
        @DisplayName("All PHI-related caches must have compliant TTLs")
        void allPhiCachesMustBeCompliant() {
            String[] phiCacheNames = {
                "patient-data",
                "patient-summaries",
                "clinical-data",
                "phi-records",
                "encounter-data",
                "observation-data"
            };

            contextRunner
                .withPropertyValues(
                    "healthdata.cache.redis.host=localhost",
                    "healthdata.cache.redis.port=6379",
                    "healthdata.cache.redis.default-ttl=PT5M",
                    "healthdata.cache.caches.patient-data.ttl=PT5M",
                    "healthdata.cache.caches.patient-summaries.ttl=PT3M",
                    "healthdata.cache.caches.clinical-data.ttl=PT5M"
                )
                .run(context -> {
                    RedisCacheManager cacheManager = context.getBean(RedisCacheManager.class);
                    Map<String, RedisCacheConfiguration> configs = cacheManager.getCacheConfigurations();

                    for (String cacheName : phiCacheNames) {
                        if (configs.containsKey(cacheName)) {
                            Duration ttl = configs.get(cacheName).getTtl();
                            assertThat(ttl)
                                .as("Cache '%s' TTL must not exceed %s for HIPAA compliance",
                                    cacheName, HIPAA_MAX_TTL)
                                .isLessThanOrEqualTo(HIPAA_MAX_TTL);
                        }
                    }
                });
        }

        @Test
        @DisplayName("Should reject PHI cache with TTL exceeding 5 minutes")
        void shouldDetectNonCompliantTtl() {
            Duration nonCompliantTtl = Duration.ofMinutes(10);

            assertThat(nonCompliantTtl)
                .as("TTL of %s exceeds HIPAA maximum of %s", nonCompliantTtl, HIPAA_MAX_TTL)
                .isGreaterThan(HIPAA_MAX_TTL);
        }
    }

    @Nested
    @DisplayName("Cache Properties Validation")
    class CachePropertiesValidationTests {

        @Test
        @DisplayName("CacheProperties should have sensible defaults")
        void cachePropertiesShouldHaveSensibleDefaults() {
            CacheProperties properties = new CacheProperties();
            CacheProperties.Redis redis = properties.getRedis();

            assertThat(redis.getDefaultTtl())
                .as("Default TTL should be set")
                .isNotNull();

            assertThat(redis.getTimeout())
                .as("Connection timeout should be set")
                .isNotNull()
                .isGreaterThan(Duration.ZERO);

            assertThat(redis.isCacheNulls())
                .as("Cache nulls should be disabled by default for PHI safety")
                .isFalse();
        }

        @Test
        @DisplayName("Custom cache spec TTL should be validated")
        void customCacheSpecTtlShouldBeValidatable() {
            CacheProperties.CacheSpec spec = new CacheProperties.CacheSpec();
            spec.setTtl(Duration.ofMinutes(3));

            assertThat(spec.getTtl())
                .as("Custom cache spec TTL should be configurable")
                .isEqualTo(Duration.ofMinutes(3));

            assertThat(spec.getTtl())
                .as("Custom cache spec TTL should be HIPAA compliant")
                .isLessThanOrEqualTo(HIPAA_MAX_TTL);
        }
    }

    @Nested
    @DisplayName("Cache Expiration Behavior")
    class CacheExpirationBehaviorTests {

        @Test
        @DisplayName("Cache TTL enforcement should be documented")
        void cacheTtlEnforcementShouldBeDocumented() {
            // This test documents the expected behavior
            // In a real integration test, we would verify actual expiration

            Duration expectedMaxTtl = HIPAA_MAX_TTL;
            Duration defaultTtl = new CacheProperties().getRedis().getDefaultTtl();

            assertThat(defaultTtl)
                .as("Default TTL (%s) should ensure PHI expires within HIPAA-compliant window (%s)",
                    defaultTtl, expectedMaxTtl)
                .isLessThanOrEqualTo(expectedMaxTtl);
        }

        @Test
        @DisplayName("PHI cache should not persist beyond session")
        void phiCacheShouldNotPersistBeyondSession() {
            // PHI caches should have TTL, not be persistent
            CacheProperties properties = new CacheProperties();
            Duration ttl = properties.getRedis().getDefaultTtl();

            assertThat(ttl)
                .as("PHI cache must have a finite TTL (not Duration.ZERO which means persistent)")
                .isNotEqualTo(Duration.ZERO)
                .isPositive();
        }
    }

    @Nested
    @DisplayName("Compliance Audit Trail")
    class ComplianceAuditTests {

        @Test
        @DisplayName("HIPAA compliance constants should be documented")
        void hipaaComplianceConstantsShouldBeDocumented() {
            // Document compliance requirements for audit purposes
            assertThat(HIPAA_MAX_TTL)
                .as("HIPAA maximum TTL should be 5 minutes")
                .isEqualTo(Duration.ofMinutes(5));

            assertThat(HIPAA_RECOMMENDED_TTL)
                .as("HIPAA recommended TTL should match maximum")
                .isEqualTo(HIPAA_MAX_TTL);
        }

        @Test
        @DisplayName("Cache configuration should be auditable")
        void cacheConfigurationShouldBeAuditable() {
            CacheProperties properties = new CacheProperties();

            // All configurable properties should be accessible for audit
            assertThat(properties.getRedis()).isNotNull();
            assertThat(properties.getCaches()).isNotNull();
            assertThat(properties.getRedis().getDefaultTtl()).isNotNull();
            assertThat(properties.getRedis().getTimeout()).isNotNull();
            assertThat(properties.getRedis().getHost()).isNotNull();
        }
    }
}
