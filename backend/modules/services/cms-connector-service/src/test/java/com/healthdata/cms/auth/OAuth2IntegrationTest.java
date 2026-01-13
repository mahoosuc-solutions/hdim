package com.healthdata.cms.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cms.dto.OAuth2TokenResponse;
import com.healthdata.cms.exception.CmsApiException;
import com.healthdata.cms.model.CmsApiProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Integration tests for OAuth2Manager with mock CMS OAuth2 endpoints
 */
@RestClientTest(OAuth2Manager.class)
@TestPropertySource(properties = {
    "cms.oauth2.client-id=test-client",
    "cms.oauth2.client-secret=test-secret",
    "cms.oauth2.token-endpoint=http://localhost:8888/oauth/token",
    "cms.oauth2.scopes=beneficiary-claims",
    "cms.oauth2.token-refresh-interval=3000000"
})
@DisplayName("OAuth2Manager Integration Tests")
class OAuth2IntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MockRestServiceServer mockServer;

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

        mockServer.expect(anything())
            .andRespond(withSuccess(
                objectMapper.writeValueAsString(expectedResponse),
                MediaType.APPLICATION_JSON
            ));

        // When
        String token = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);

        // Then
        assertNotNull(token);
        assertEquals("mock-cms-token-12345", token);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle 401 Unauthorized from OAuth2 endpoint")
    void testOAuth2EndpointUnauthorized() {
        // Given
        mockServer.expect(anything())
            .andRespond(withUnauthorizedRequest());

        // When & Then
        CmsApiException exception = assertThrows(CmsApiException.class,
            () -> oauth2Manager.getAccessToken(CmsApiProvider.DPC));

        assertTrue(exception.getMessage().contains("OAuth2 token exchange failed"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle 503 Service Unavailable from OAuth2 endpoint")
    void testOAuth2EndpointServiceUnavailable() {
        // Given
        mockServer.expect(anything())
            .andRespond(withServerError());

        // When & Then
        CmsApiException exception = assertThrows(CmsApiException.class,
            () -> oauth2Manager.getAccessToken(CmsApiProvider.BCDA));

        assertTrue(exception.getMessage().contains("OAuth2 token exchange failed"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle malformed JSON response from OAuth2 endpoint")
    void testOAuth2EndpointMalformedJson() {
        // Given
        mockServer.expect(anything())
            .andRespond(withSuccess(
                "{ invalid json }",
                MediaType.APPLICATION_JSON
            ));

        // When & Then
        CmsApiException exception = assertThrows(CmsApiException.class,
            () -> oauth2Manager.getAccessToken(CmsApiProvider.DPC));

        assertTrue(exception.getMessage().contains("OAuth2 token exchange failed"));
        mockServer.verify();
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

        mockServer.expect(anything())
            .andRespond(withSuccess(
                objectMapper.writeValueAsString(bcdaResponse),
                MediaType.APPLICATION_JSON
            ));
        
        mockServer.expect(anything())
            .andRespond(withSuccess(
                objectMapper.writeValueAsString(dpcResponse),
                MediaType.APPLICATION_JSON
            ));

        // When
        String bcdaToken = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);
        String dpcToken = oauth2Manager.getAccessToken(CmsApiProvider.DPC);

        // Then
        assertEquals("bcda-token-xyz", bcdaToken);
        assertEquals("dpc-token-abc", dpcToken);
        assertNotEquals(bcdaToken, dpcToken);
        mockServer.verify();
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

        // Setup mock to respond only once
        mockServer.expect(anything())
            .andRespond(withSuccess(
                objectMapper.writeValueAsString(response),
                MediaType.APPLICATION_JSON
            ));

        // When
        String firstCall = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);
        String secondCall = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);

        // Then - both calls should return same token from cache
        assertEquals(firstCall, secondCall);
        assertEquals("cached-token-123", firstCall);
        // Mock was only called once (not twice), proving caching works
        mockServer.verify();
    }

    @Test
    @DisplayName("Should validate token response structure")
    void testTokenResponseValidation() throws Exception {
        // Given - missing required field (expiresIn)
        String incompleteResponse = "{ \"access_token\": \"token\", \"token_type\": \"Bearer\" }";

        mockServer.expect(anything())
            .andRespond(withSuccess(incompleteResponse, MediaType.APPLICATION_JSON));

        // When & Then
        CmsApiException exception = assertThrows(CmsApiException.class,
            () -> oauth2Manager.getAccessToken(CmsApiProvider.DPC));

        assertTrue(exception.getMessage().contains("Invalid OAuth2 token response"));
        mockServer.verify();
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

        mockServer.expect(anything())
            .andRespond(withSuccess(
                objectMapper.writeValueAsString(response),
                MediaType.APPLICATION_JSON
            ));

        // When
        oauth2Manager.getAccessToken(CmsApiProvider.BCDA);
        OAuth2Manager.TokenInfo tokenInfo = oauth2Manager.getTokenInfo(CmsApiProvider.BCDA);

        // Then
        assertNotNull(tokenInfo);
        assertEquals("metadata-test-token", tokenInfo.getAccessToken());
        assertEquals(CmsApiProvider.BCDA, tokenInfo.getProvider());
        assertNotNull(tokenInfo.getExpiresAt());
        mockServer.verify();
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

        mockServer.expect(anything())
            .andRespond(withSuccess(
                objectMapper.writeValueAsString(response),
                MediaType.APPLICATION_JSON
            ));

        // When - make multiple requests rapidly
        for (int i = 0; i < 5; i++) {
            String token = oauth2Manager.getAccessToken(CmsApiProvider.BCDA);
            assertNotNull(token);
        }

        // Then - only one actual API call should have been made (cached)
        mockServer.verify();
    }
}
