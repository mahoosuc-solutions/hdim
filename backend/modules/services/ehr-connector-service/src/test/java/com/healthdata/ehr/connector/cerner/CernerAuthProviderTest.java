package com.healthdata.ehr.connector.cerner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.ehr.connector.cerner.config.CernerConnectionConfig;
import com.healthdata.ehr.connector.cerner.model.CernerTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CernerAuthProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CacheManager cacheManager;

    private CernerConnectionConfig config;
    private CernerAuthProvider authProvider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        config = new CernerConnectionConfig();
        config.setTokenUrl("https://authorization.cerner.com/oauth2/token");
        config.setClientId("test-client-id");
        config.setClientSecret("test-client-secret");
        config.setScope("system/*.read");
        config.setTokenCacheDuration(3600L);

        objectMapper = new ObjectMapper();
        authProvider = new CernerAuthProvider(restTemplate, config, cacheManager, objectMapper);
    }

    @Test
    void testGetAccessToken_Success() {
        // Arrange
        CernerTokenResponse tokenResponse = CernerTokenResponse.builder()
                .accessToken("test-access-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .scope("system/*.read")
                .issuedAt(Instant.now())
                .build();

        when(cacheManager.getCache("cernerTokens")).thenReturn(new ConcurrentMapCache("cernerTokens"));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(CernerTokenResponse.class)
        )).thenReturn(ResponseEntity.ok(tokenResponse));

        // Act
        String accessToken = authProvider.getAccessToken();

        // Assert
        assertNotNull(accessToken);
        assertEquals("test-access-token", accessToken);
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(CernerTokenResponse.class)
        );
    }

    @Test
    void testGetAccessToken_ReturnsFromCacheWhenValid() {
        // Arrange
        CernerTokenResponse cachedToken = CernerTokenResponse.builder()
                .accessToken("cached-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .issuedAt(Instant.now())
                .build();

        ConcurrentMapCache cache = new ConcurrentMapCache("cernerTokens");
        cache.put("token", cachedToken);
        when(cacheManager.getCache("cernerTokens")).thenReturn(cache);

        // Act
        String accessToken = authProvider.getAccessToken();

        // Assert
        assertEquals("cached-token", accessToken);
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(CernerTokenResponse.class));
    }

    @Test
    void testGetAccessToken_RefreshesExpiredToken() {
        // Arrange
        CernerTokenResponse expiredToken = CernerTokenResponse.builder()
                .accessToken("expired-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .issuedAt(Instant.now().minusSeconds(3700))
                .build();

        CernerTokenResponse newToken = CernerTokenResponse.builder()
                .accessToken("new-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .issuedAt(Instant.now())
                .build();

        ConcurrentMapCache cache = new ConcurrentMapCache("cernerTokens");
        cache.put("token", expiredToken);
        when(cacheManager.getCache("cernerTokens")).thenReturn(cache);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(CernerTokenResponse.class)
        )).thenReturn(ResponseEntity.ok(newToken));

        // Act
        String accessToken = authProvider.getAccessToken();

        // Assert
        assertEquals("new-token", accessToken);
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(CernerTokenResponse.class));
    }

    @Test
    void testGetAccessToken_UsesClientCredentialsGrant() {
        // Arrange
        CernerTokenResponse tokenResponse = CernerTokenResponse.builder()
                .accessToken("test-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .issuedAt(Instant.now())
                .build();

        when(cacheManager.getCache("cernerTokens")).thenReturn(new ConcurrentMapCache("cernerTokens"));
        
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                requestCaptor.capture(),
                eq(CernerTokenResponse.class)
        )).thenReturn(ResponseEntity.ok(tokenResponse));

        // Act
        authProvider.getAccessToken();

        // Assert
        HttpEntity<?> capturedRequest = requestCaptor.getValue();
        assertNotNull(capturedRequest);
        @SuppressWarnings("unchecked")
        MultiValueMap<String, String> body = (MultiValueMap<String, String>) capturedRequest.getBody();
        assertEquals("client_credentials", body.getFirst("grant_type"));
    }

    @Test
    void testGetAccessToken_IncludesBasicAuthHeader() {
        // Arrange
        CernerTokenResponse tokenResponse = CernerTokenResponse.builder()
                .accessToken("test-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .issuedAt(Instant.now())
                .build();

        when(cacheManager.getCache("cernerTokens")).thenReturn(new ConcurrentMapCache("cernerTokens"));
        
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                requestCaptor.capture(),
                eq(CernerTokenResponse.class)
        )).thenReturn(ResponseEntity.ok(tokenResponse));

        // Act
        authProvider.getAccessToken();

        // Assert
        HttpEntity<?> capturedRequest = requestCaptor.getValue();
        HttpHeaders headers = capturedRequest.getHeaders();
        assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));
        assertTrue(headers.getFirst(HttpHeaders.AUTHORIZATION).startsWith("Basic "));
    }

    @Test
    void testGetAccessToken_ThrowsExceptionOnFailure() {
        // Arrange
        when(cacheManager.getCache("cernerTokens")).thenReturn(new ConcurrentMapCache("cernerTokens"));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(CernerTokenResponse.class)
        )).thenThrow(new RestClientException("Connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authProvider.getAccessToken());
    }

    @Test
    void testGetAuthorizationHeader_ReturnsFormattedHeader() {
        // Arrange
        CernerTokenResponse tokenResponse = CernerTokenResponse.builder()
                .accessToken("test-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .issuedAt(Instant.now())
                .build();

        when(cacheManager.getCache("cernerTokens")).thenReturn(new ConcurrentMapCache("cernerTokens"));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(CernerTokenResponse.class)
        )).thenReturn(ResponseEntity.ok(tokenResponse));

        // Act
        String authHeader = authProvider.getAuthorizationHeader();

        // Assert
        assertEquals("Bearer test-token", authHeader);
    }

    @Test
    void testClearTokenCache_RemovesToken() {
        // Arrange
        ConcurrentMapCache cache = new ConcurrentMapCache("cernerTokens");
        cache.put("token", CernerTokenResponse.builder().accessToken("test").build());
        when(cacheManager.getCache("cernerTokens")).thenReturn(cache);

        // Act
        authProvider.clearTokenCache();

        // Assert
        assertNull(cache.get("token"));
    }

    @Test
    void testGetAccessToken_HandlesNullResponse() {
        // Arrange
        when(cacheManager.getCache("cernerTokens")).thenReturn(new ConcurrentMapCache("cernerTokens"));
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(CernerTokenResponse.class)
        )).thenReturn(ResponseEntity.ok(null));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authProvider.getAccessToken());
    }

    @Test
    void testGetAccessToken_IncludesRequestedScope() {
        // Arrange
        CernerTokenResponse tokenResponse = CernerTokenResponse.builder()
                .accessToken("test-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .scope("system/*.read")
                .issuedAt(Instant.now())
                .build();

        when(cacheManager.getCache("cernerTokens")).thenReturn(new ConcurrentMapCache("cernerTokens"));
        
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                requestCaptor.capture(),
                eq(CernerTokenResponse.class)
        )).thenReturn(ResponseEntity.ok(tokenResponse));

        // Act
        authProvider.getAccessToken();

        // Assert
        HttpEntity<?> capturedRequest = requestCaptor.getValue();
        @SuppressWarnings("unchecked")
        MultiValueMap<String, String> body = (MultiValueMap<String, String>) capturedRequest.getBody();
        assertNotNull(body.getFirst("scope"));
        assertTrue(body.getFirst("scope").contains("system"));
    }

    @Test
    void testIsTokenValid_ReturnsTrueForValidToken() {
        // Arrange
        CernerTokenResponse validToken = CernerTokenResponse.builder()
                .accessToken("valid-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .issuedAt(Instant.now())
                .build();

        // Act
        boolean isValid = authProvider.isTokenValid(validToken);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testIsTokenValid_ReturnsFalseForExpiredToken() {
        // Arrange
        CernerTokenResponse expiredToken = CernerTokenResponse.builder()
                .accessToken("expired-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .issuedAt(Instant.now().minusSeconds(4000))
                .build();

        // Act
        boolean isValid = authProvider.isTokenValid(expiredToken);

        // Assert
        assertFalse(isValid);
    }
}
