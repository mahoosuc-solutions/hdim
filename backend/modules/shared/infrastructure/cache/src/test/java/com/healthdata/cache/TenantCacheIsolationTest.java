package com.healthdata.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.persistence.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Tenant Cache Isolation Tests (Key Prefix + HIPAA TTL)")
class TenantCacheIsolationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
        .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
        .withBean(ObjectMapper.class, ObjectMapper::new);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Key prefix should include current tenant ID to prevent cross-tenant collisions")
    void keyPrefixShouldIncludeTenantId() {
        TenantContext.setCurrentTenant("tenant-a");

        contextRunner.run(context -> {
            RedisCacheConfiguration cfg = context.getBean(RedisCacheConfiguration.class);
            assertThat(cfg.getKeyPrefixFor("patient-data")).startsWith("tenant-a:patient-data::");
        });
    }

    @Test
    @DisplayName("Different tenants should produce different key prefixes for the same cache")
    void differentTenantsShouldProduceDifferentPrefixes() {
        contextRunner.run(context -> {
            RedisCacheConfiguration cfg = context.getBean(RedisCacheConfiguration.class);

            TenantContext.setCurrentTenant("tenant-a");
            String prefixA = cfg.getKeyPrefixFor("patient-data");

            TenantContext.setCurrentTenant("tenant-b");
            String prefixB = cfg.getKeyPrefixFor("patient-data");

            assertThat(prefixA).isNotEqualTo(prefixB);
            assertThat(prefixA).startsWith("tenant-a:patient-data::");
            assertThat(prefixB).startsWith("tenant-b:patient-data::");
        });
    }

    @Test
    @DisplayName("When tenant is missing, prefix should use a safe no-tenant namespace")
    void missingTenantShouldUseNoTenantPrefix() {
        TenantContext.clear();

        contextRunner.run(context -> {
            RedisCacheConfiguration cfg = context.getBean(RedisCacheConfiguration.class);
            assertThat(cfg.getKeyPrefixFor("patient-data")).startsWith("no-tenant:patient-data::");
        });
    }

    @Test
    @DisplayName("Blank tenant IDs should be treated as missing tenant context")
    void blankTenantShouldUseNoTenantPrefix() {
        TenantContext.setCurrentTenant("   ");

        contextRunner.run(context -> {
            RedisCacheConfiguration cfg = context.getBean(RedisCacheConfiguration.class);
            assertThat(cfg.getKeyPrefixFor("patient-data")).startsWith("no-tenant:patient-data::");
        });
    }

    @Test
    @DisplayName("Clearing tenant context should change key prefix back to no-tenant namespace")
    void clearingTenantContextShouldResetPrefix() {
        contextRunner.run(context -> {
            RedisCacheConfiguration cfg = context.getBean(RedisCacheConfiguration.class);

            TenantContext.setCurrentTenant("tenant-a");
            assertThat(cfg.getKeyPrefixFor("patient-data")).startsWith("tenant-a:patient-data::");

            TenantContext.clear();
            assertThat(cfg.getKeyPrefixFor("patient-data")).startsWith("no-tenant:patient-data::");
        });
    }

    @Test
    @DisplayName("Key prefix format should be deterministic and include cache name separator")
    void keyPrefixFormatShouldBeDeterministic() {
        TenantContext.setCurrentTenant("tenant-a");

        contextRunner.run(context -> {
            RedisCacheConfiguration cfg = context.getBean(RedisCacheConfiguration.class);
            String prefix = cfg.getKeyPrefixFor("patient-data");
            assertThat(prefix).isEqualTo("tenant-a:patient-data::");
        });
    }

    @Test
    @DisplayName("System mode should use a dedicated namespace to prevent accidental mixing with tenant keys")
    void systemModeShouldUseSystemPrefix() {
        TenantContext.enableSystemMode();

        contextRunner.run(context -> {
            RedisCacheConfiguration cfg = context.getBean(RedisCacheConfiguration.class);
            assertThat(cfg.getKeyPrefixFor("patient-data")).startsWith("system:patient-data::");
        });
    }

    @Test
    @DisplayName("Configured caches should keep tenant key prefixing")
    void configuredCachesShouldKeepTenantPrefixing() {
        TenantContext.setCurrentTenant("tenant-a");

        contextRunner
            .withPropertyValues(
                "healthdata.cache.redis.host=localhost",
                "healthdata.cache.redis.port=6379",
                "healthdata.cache.redis.default-ttl=PT5M",
                "healthdata.cache.caches.patient-data.ttl=PT5M",
                "healthdata.cache.caches.patient-summaries.ttl=PT3M"
            )
            .run(context -> {
                RedisCacheManager cacheManager = context.getBean(RedisCacheManager.class);
                Map<String, RedisCacheConfiguration> configs = cacheManager.getCacheConfigurations();

                assertThat(configs).containsKeys("patient-data", "patient-summaries");

                assertThat(configs.get("patient-data").getKeyPrefixFor("patient-data"))
                    .startsWith("tenant-a:patient-data::");
                assertThat(configs.get("patient-summaries").getKeyPrefixFor("patient-summaries"))
                    .startsWith("tenant-a:patient-summaries::");
            });
    }

    @Test
    @DisplayName("PHI cache TTL must not exceed 5 minutes (HIPAA policy)")
    void phiCacheTtlMustNotExceedFiveMinutes() {
        Duration hipaaMax = Duration.ofMinutes(5);

        contextRunner
            .withPropertyValues(
                "healthdata.cache.redis.host=localhost",
                "healthdata.cache.redis.port=6379",
                "healthdata.cache.caches.patient-data.ttl=PT5M"
            )
            .run(context -> {
                RedisCacheManager cacheManager = context.getBean(RedisCacheManager.class);
                Map<String, RedisCacheConfiguration> configs = cacheManager.getCacheConfigurations();
                assertThat(configs.get("patient-data").getTtl()).isLessThanOrEqualTo(hipaaMax);
            });
    }
}
