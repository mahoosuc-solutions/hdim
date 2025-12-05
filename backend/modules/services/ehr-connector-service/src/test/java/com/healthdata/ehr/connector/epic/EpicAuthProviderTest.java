package com.healthdata.ehr.connector.epic;

import com.healthdata.ehr.connector.core.EhrConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpicAuthProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private EpicConnectionConfig config;

    private EpicAuthProvider authProvider;
    private PrivateKey testPrivateKey;

    @BeforeEach
    void setUp() throws Exception {
        // Generate test RSA key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        testPrivateKey = keyPair.getPrivate();

        when(config.getClientId()).thenReturn("test-client-id");
        when(config.getTokenUrl()).thenReturn("https://fhir.epic.com/oauth2/token");
        when(config.getPrivateKey()).thenReturn(testPrivateKey);
        when(config.getTokenCacheDurationMinutes()).thenReturn(50);

        authProvider = new EpicAuthProvider(config, restTemplate);
    }

    @Test
    void testGetAccessToken_Success() {
        // Arrange
        EpicTokenResponse tokenResponse = new EpicTokenResponse();
        tokenResponse.setAccessToken("test-access-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600);
        tokenResponse.setObtainedAt(Instant.now());

        ResponseEntity<EpicTokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        )).thenReturn(responseEntity);

        // Act
        String accessToken = authProvider.getAccessToken();

        // Assert
        assertEquals("test-access-token", accessToken);
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        );
    }

    @Test
    void testGetAccessToken_UsesCache_WhenTokenValid() {
        // Arrange
        EpicTokenResponse tokenResponse = new EpicTokenResponse();
        tokenResponse.setAccessToken("cached-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600);
        tokenResponse.setObtainedAt(Instant.now());

        ResponseEntity<EpicTokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        )).thenReturn(responseEntity);

        // Act
        String firstToken = authProvider.getAccessToken();
        String secondToken = authProvider.getAccessToken();

        // Assert
        assertEquals(firstToken, secondToken);
        // Should only call the API once due to caching
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        );
    }

    @Test
    void testGetAccessToken_RefreshesExpiredToken() {
        // Arrange
        EpicTokenResponse expiredToken = new EpicTokenResponse();
        expiredToken.setAccessToken("expired-token");
        expiredToken.setTokenType("Bearer");
        expiredToken.setExpiresIn(1); // 1 second expiry
        expiredToken.setObtainedAt(Instant.now().minusSeconds(10)); // Obtained 10 seconds ago

        EpicTokenResponse freshToken = new EpicTokenResponse();
        freshToken.setAccessToken("fresh-token");
        freshToken.setTokenType("Bearer");
        freshToken.setExpiresIn(3600);
        freshToken.setObtainedAt(Instant.now());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        )).thenReturn(new ResponseEntity<>(expiredToken, HttpStatus.OK))
          .thenReturn(new ResponseEntity<>(freshToken, HttpStatus.OK));

        // Act
        String firstToken = authProvider.getAccessToken();
        String secondToken = authProvider.getAccessToken();

        // Assert
        assertEquals("fresh-token", secondToken);
        verify(restTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        );
    }

    @Test
    void testGetAccessToken_ThrowsException_OnHttpError() {
        // Arrange
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        // Act & Assert
        EhrConnectionException exception = assertThrows(
                EhrConnectionException.class,
                () -> authProvider.getAccessToken()
        );

        assertTrue(exception.getMessage().contains("Failed to obtain access token"));
        assertEquals("Epic", exception.getEhrSystem());
        assertEquals(401, exception.getStatusCode());
    }

    @Test
    void testRefreshToken_Success() {
        // Arrange
        EpicTokenResponse tokenResponse = new EpicTokenResponse();
        tokenResponse.setAccessToken("refreshed-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600);
        tokenResponse.setObtainedAt(Instant.now());

        ResponseEntity<EpicTokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        )).thenReturn(responseEntity);

        // Act
        String refreshedToken = authProvider.refreshToken();

        // Assert
        assertEquals("refreshed-token", refreshedToken);
    }

    @Test
    void testIsTokenValid_ReturnsFalse_WhenNoToken() {
        // Act & Assert
        assertFalse(authProvider.isTokenValid());
    }

    @Test
    void testIsTokenValid_ReturnsTrue_WhenTokenValid() {
        // Arrange
        EpicTokenResponse tokenResponse = new EpicTokenResponse();
        tokenResponse.setAccessToken("valid-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600);
        tokenResponse.setObtainedAt(Instant.now());

        ResponseEntity<EpicTokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        )).thenReturn(responseEntity);

        authProvider.getAccessToken();

        // Act & Assert
        assertTrue(authProvider.isTokenValid());
    }

    @Test
    void testIsTokenValid_ReturnsFalse_WhenTokenExpired() {
        // Arrange
        EpicTokenResponse tokenResponse = new EpicTokenResponse();
        tokenResponse.setAccessToken("expired-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(1);
        tokenResponse.setObtainedAt(Instant.now().minusSeconds(10));

        ResponseEntity<EpicTokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        )).thenReturn(responseEntity);

        authProvider.getAccessToken();

        // Act & Assert
        assertFalse(authProvider.isTokenValid());
    }

    @Test
    void testInvalidateToken_ClearsCache() {
        // Arrange
        EpicTokenResponse tokenResponse = new EpicTokenResponse();
        tokenResponse.setAccessToken("token-to-invalidate");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600);
        tokenResponse.setObtainedAt(Instant.now());

        ResponseEntity<EpicTokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        )).thenReturn(responseEntity);

        authProvider.getAccessToken();

        // Act
        authProvider.invalidateToken();

        // Assert
        assertFalse(authProvider.isTokenValid());
    }

    @Test
    void testCreateJwtAssertion_ContainsRequiredClaims() {
        // Act
        String jwt = authProvider.createJwtAssertion();

        // Assert
        assertNotNull(jwt);
        assertTrue(jwt.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void testGetAccessToken_WithInvalidPrivateKey_ThrowsException() {
        // Arrange
        when(config.getPrivateKey()).thenReturn(null);
        EpicAuthProvider invalidAuthProvider = new EpicAuthProvider(config, restTemplate);

        // Act & Assert
        assertThrows(EhrConnectionException.class, () -> invalidAuthProvider.getAccessToken());
    }

    @Test
    void testGetAccessToken_WithNullResponse_ThrowsException() {
        // Arrange
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // Act & Assert
        assertThrows(EhrConnectionException.class, () -> authProvider.getAccessToken());
    }

    @Test
    void testGetAccessToken_RetryOnRateLimit() {
        // Arrange
        HttpClientErrorException rateLimitException =
            new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");

        EpicTokenResponse tokenResponse = new EpicTokenResponse();
        tokenResponse.setAccessToken("retry-success-token");
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresIn(3600);
        tokenResponse.setObtainedAt(Instant.now());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        )).thenThrow(rateLimitException)
          .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        // Act
        String token = authProvider.getAccessToken();

        // Assert
        assertEquals("retry-success-token", token);
        verify(restTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(EpicTokenResponse.class)
        );
    }
}
