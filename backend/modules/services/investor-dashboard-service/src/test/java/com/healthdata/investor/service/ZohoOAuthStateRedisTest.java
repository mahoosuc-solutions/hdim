package com.healthdata.investor.service;

import com.healthdata.investor.exception.AuthenticationException;
import com.healthdata.investor.repository.InvestorUserRepository;
import com.healthdata.investor.repository.ZohoConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies Zoho OAuth state is stored in Redis with TTL (B5).
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ZohoOAuthStateRedisTest {

    @Mock
    private ZohoConnectionRepository connectionRepository;

    @Mock
    private InvestorUserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private ZohoOAuthService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new ZohoOAuthService(connectionRepository, userRepository, redisTemplate);
    }

    @Test
    void getAuthorizationUrl_shouldStoreStateInRedisWithTTL() {
        // Enable Zoho via reflection since @Value fields aren't injected in unit tests
        setField(service, "enabled", true);
        setField(service, "clientId", "test-client-id");
        setField(service, "scope", "ZohoCRM.modules.ALL");
        setField(service, "redirectUri", "http://localhost/callback");
        setField(service, "accountsUrl", "https://accounts.zoho.com");

        service.getAuthorizationUrl("test-state-123");

        verify(valueOps).set(
            eq("zoho:oauth:state:test-state-123"),
            eq("test-state-123"),
            eq(Duration.ofMinutes(10))
        );
    }

    @Test
    void handleOAuthCallback_shouldRejectMissingState() {
        when(valueOps.getAndDelete("zoho:oauth:state:invalid-state")).thenReturn(null);

        assertThatThrownBy(() ->
            service.handleOAuthCallback("code", "invalid-state", "zoho.com", java.util.UUID.randomUUID())
        )
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("expired state token");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
