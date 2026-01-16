package com.healthdata.cms.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cms.dto.OAuth2TokenResponse;
import com.healthdata.cms.exception.CmsApiException;
import com.healthdata.cms.model.CmsApiProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Integration tests for OAuth2Manager with mock CMS OAuth2 endpoints
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2Manager Integration Tests")
class OAuth2IntegrationTest {

    @Mock
    private RestTemplate restTemplate;

    private OAuth2Manager oauth2Manager;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        oauth2Manager = new OAuth2Manager(restTemplate);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should successfully obtain token from mock CMS OAuth2 endpoint")
    void testObtainTokenFromMockEndpoint() throws Exception {
        // Given
        OAuth2TokenResponse expectedResponse = OAuth2TokenResponse.builder()
            .accessToken("mock-cms-token-12345")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .scope("beneficiary-claims")
            .build();

        ResponseEntity<OAuth2TokenResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OAuth2TokenResponse.class)))
            .thenReturn(responseEntity);

        // When
        String token = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);

        // Then
        assertNotNull(token);
        assertEquals("mock-cms-token-12345", token);
    }

    @Test
    @DisplayName("Should handle 401 Unauthorized from OAuth2 endpoint")
    void testOAuth2EndpointUnauthorized() {
        // Given
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OAuth2TokenResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        // When & Then
        CmsApiException exception = assertThrows(CmsApiException.class,
            () -> oauth2Manager.getAccessToken(CmsApiProvider.DPC));

        assertTrue(exception.getMessage().contains("OAuth2 token request failed") || 
                   exception.getMessage().contains("401"));
    }

    @Test
    @DisplayName("Should handle 503 Service Unavailable from OAuth2 endpoint")
    void testOAuth2EndpointServiceUnavailable() {
        // Given
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OAuth2TokenResponse.class)))
            .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable"));

        // When & Then
        CmsApiException exception = assertThrows(CmsApiException.class,
            () -> oauth2Manager.getAccessToken(CmsApiProvider.BCDA));

        assertTrue(exception.getMessage().contains("OAuth2 server error") || 
                   exception.getMessage().contains("503"));
    }

    @Test
    @DisplayName("Should handle malformed JSON response from OAuth2 endpoint")
    void testOAuth2EndpointMalformedJson() {
        // Given
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OAuth2TokenResponse.class)))
            .thenThrow(new RestClientException("Error parsing response"));

        // When & Then
        CmsApiException exception = assertThrows(CmsApiException.class,
            () -> oauth2Manager.getAccessToken(CmsApiProvider.DPC));

        assertTrue(exception.getMessage().contains("Network error during OAuth2 token exchange"));
    }

    @Test
    @DisplayName("Should obtain tokens for different CMS API providers")
    void testObtainTokensForMultipleProviders() throws Exception {
        // Given
        OAuth2TokenResponse bcdaResponse = OAuth2TokenResponse.builder()
            .accessToken("bcda-token-xyz")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .build();

        OAuth2TokenResponse dpcResponse = OAuth2TokenResponse.builder()
            .accessToken("dpc-token-abc")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .build();

        ResponseEntity<OAuth2TokenResponse> bcdaEntity = new ResponseEntity<>(bcdaResponse, HttpStatus.OK);
        ResponseEntity<OAuth2TokenResponse> dpcEntity = new ResponseEntity<>(dpcResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OAuth2TokenResponse.class)))
            .thenReturn(bcdaEntity)
            .thenReturn(dpcEntity);

        // When
        String bcdaToken = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);
        String dpcToken = oauth2Manager.getAccessToken(CmsApiProvider.DPC);

        // Then
        assertEquals("bcda-token-xyz", bcdaToken);
        assertEquals("dpc-token-abc", dpcToken);
        assertNotEquals(bcdaToken, dpcToken);
    }

    @Test
    @DisplayName("Should cache and reuse tokens for same provider")
    void testTokenCachingAndReuse() throws Exception {
        // Given
        OAuth2TokenResponse response = OAuth2TokenResponse.builder()
            .accessToken("cached-token-123")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .build();

        ResponseEntity<OAuth2TokenResponse> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OAuth2TokenResponse.class)))
            .thenReturn(responseEntity);

        // When
        String firstCall = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);
        String secondCall = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);

        // Then - both calls should return same token from cache
        assertEquals(firstCall, secondCall);
        assertEquals("cached-token-123", firstCall);
    }

    @Test
    @DisplayName("Should validate token response structure")
    void testTokenResponseValidation() throws Exception {
        // Given - null response body
        ResponseEntity<OAuth2TokenResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OAuth2TokenResponse.class)))
            .thenReturn(responseEntity);

        // When & Then
        CmsApiException exception = assertThrows(CmsApiException.class,
            () -> oauth2Manager.getAccessToken(CmsApiProvider.DPC));

        assertTrue(exception.getMessage().contains("Empty response from OAuth2 token endpoint"));
    }

    @Test
    @DisplayName("Should store and retrieve token metadata")
    void testTokenMetadataStorage() throws Exception {
        // Given
        OAuth2TokenResponse response = OAuth2TokenResponse.builder()
            .accessToken("metadata-test-token")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .scope("beneficiary-claims quality-measures")
            .build();

        ResponseEntity<OAuth2TokenResponse> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OAuth2TokenResponse.class)))
            .thenReturn(responseEntity);

        // When
        oauth2Manager.getAccessToken(CmsApiProvider.BCDA);
        OAuth2Manager.TokenInfo tokenInfo = oauth2Manager.getTokenInfo(CmsApiProvider.BCDA);

        // Then
        assertNotNull(tokenInfo);
        assertEquals("metadata-test-token", tokenInfo.getAccessToken());
        assertEquals(CmsApiProvider.BCDA, tokenInfo.getProvider());
        assertNotNull(tokenInfo.getExpiresAt());
    }

    @Test
    @DisplayName("Should handle rapid sequential token requests")
    void testRapidSequentialRequests() throws Exception {
        // Given
        OAuth2TokenResponse response = OAuth2TokenResponse.builder()
            .accessToken("rapid-test-token")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .build();

        ResponseEntity<OAuth2TokenResponse> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(OAuth2TokenResponse.class)))
            .thenReturn(responseEntity);

        // When - make multiple requests rapidly
        for (int i = 0; i < 5; i++) {
            String token = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);
            assertNotNull(token);
        }

        // Then - tokens should be cached and same
        assertEquals("rapid-test-token", oauth2Manager.getAccessToken(CmsApiProvider.BCDA));
    }
}
