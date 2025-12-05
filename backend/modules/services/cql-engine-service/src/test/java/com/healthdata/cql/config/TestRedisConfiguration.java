package com.healthdata.cql.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration to provide a mock RedisTemplate for integration tests
 *
 * This configuration ensures that tests don't require an actual Redis instance
 * and provides a properly initialized RedisTemplate bean that won't throw
 * "template not initialized" errors during tests.
 */
@TestConfiguration
public class TestRedisConfiguration {

    /**
     * Provides a fully mocked RedisTemplate for tests
     *
     * Uses a mock that returns proper stub responses for all Redis operations.
     */
    @SuppressWarnings("unchecked")
    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        // Create a mock RedisTemplate that handles all operations gracefully
        RedisTemplate<String, String> mockTemplate = mock(RedisTemplate.class);

        // Mock ValueOperations
        ValueOperations<String, String> mockValueOps = mock(ValueOperations.class);
        when(mockValueOps.get(anyString())).thenReturn(null);
        when(mockTemplate.opsForValue()).thenReturn(mockValueOps);

        // Mock key operations
        when(mockTemplate.keys(anyString())).thenReturn(Collections.emptySet());
        when(mockTemplate.delete(anyString())).thenReturn(true);
        when(mockTemplate.delete((Set<String>) any())).thenReturn(0L);

        return mockTemplate;
    }
}
