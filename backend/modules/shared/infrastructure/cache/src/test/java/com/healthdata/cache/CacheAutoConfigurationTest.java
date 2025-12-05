package com.healthdata.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

class CacheAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
            .withPropertyValues(
                    "healthdata.cache.redis.host=localhost",
                    "healthdata.cache.redis.port=6381",
                    "healthdata.cache.redis.default-ttl=PT10M",
                    "healthdata.cache.caches.patient-summaries.ttl=PT60S",
                    "healthdata.cache.caches.patient-summaries.cache-nulls=false");

    @Test
    void shouldApplyPerCacheTtlConfiguration() {
        contextRunner
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .run(context -> {
                    CacheManager cacheManager = context.getBean(CacheManager.class);
                    assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);

                    RedisCacheManager redisCacheManager = (RedisCacheManager) cacheManager;
                    @SuppressWarnings("deprecation")
                    Duration patientTtl = redisCacheManager.getCacheConfigurations()
                            .get("patient-summaries")
                            .getTtl();

                    assertThat(patientTtl).isEqualTo(Duration.ofSeconds(60));
                });
    }
}
