package com.healthdata.investor.service;

import com.healthdata.investor.dto.LinkedInDTO;
import com.healthdata.investor.entity.InvestorUser;
import com.healthdata.investor.entity.LinkedInConnection;
import com.healthdata.investor.exception.AuthenticationException;
import com.healthdata.investor.exception.ResourceNotFoundException;
import com.healthdata.investor.repository.InvestorUserRepository;
import com.healthdata.investor.repository.LinkedInConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for LinkedIn OAuth integration and API operations.
 * Enables automated tracking of LinkedIn outreach activities.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LinkedInService {

    private final LinkedInConnectionRepository connectionRepository;
    private final InvestorUserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${linkedin.oauth2.client-id}")
    private String clientId;

    @Value("${linkedin.oauth2.client-secret}")
    private String clientSecret;

    @Value("${linkedin.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${linkedin.oauth2.scope}")
    private String scope;

    @Value("${linkedin.api.base-url}")
    private String apiBaseUrl;

    /**
     * Generate the LinkedIn OAuth authorization URL.
     */
    public String getAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl("https://www.linkedin.com/oauth/v2/authorization")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    /**
     * Exchange authorization code for access token.
     */
    @Transactional
    public LinkedInDTO.ConnectionStatus handleOAuthCallback(String code, UUID userId) {
        InvestorUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        // Exchange code for token
        Map<String, String> tokenResponse = exchangeCodeForToken(code);
        String accessToken = tokenResponse.get("access_token");
        String expiresIn = tokenResponse.get("expires_in");

        // Get LinkedIn profile
        LinkedInDTO.Profile profile = fetchLinkedInProfile(accessToken);

        // Save or update connection
        LinkedInConnection connection = connectionRepository.findByUserId(userId)
                .orElse(LinkedInConnection.builder().user(user).build());

        connection.setAccessToken(accessToken);
        connection.setLinkedInMemberId(profile.getId());
        connection.setLinkedInProfileUrl(profile.getProfileUrl());
        connection.setTokenExpiresAt(Instant.now().plusSeconds(Long.parseLong(expiresIn)));
        connection.setScope(scope);
        connection.setConnected(true);
        connection.setLastSync(Instant.now());
        connection.setSyncError(null);

        connectionRepository.save(connection);
        log.info("LinkedIn connected for user: {} with profile: {}", user.getEmail(), profile.getId());

        return LinkedInDTO.ConnectionStatus.builder()
                .connected(true)
                .linkedInMemberId(profile.getId())
                .profileUrl(profile.getProfileUrl())
                .expiresAt(connection.getTokenExpiresAt())
                .build();
    }

    /**
     * Check if user has an active LinkedIn connection.
     */
    public LinkedInDTO.ConnectionStatus getConnectionStatus(UUID userId) {
        return connectionRepository.findByUserId(userId)
                .map(conn -> LinkedInDTO.ConnectionStatus.builder()
                        .connected(conn.isConnected())
                        .linkedInMemberId(conn.getLinkedInMemberId())
                        .profileUrl(conn.getLinkedInProfileUrl())
                        .expiresAt(conn.getTokenExpiresAt())
                        .lastSync(conn.getLastSync())
                        .syncError(conn.getSyncError())
                        .build())
                .orElse(LinkedInDTO.ConnectionStatus.builder()
                        .connected(false)
                        .build());
    }

    /**
     * Disconnect LinkedIn account.
     */
    @Transactional
    public void disconnect(UUID userId) {
        connectionRepository.findByUserId(userId).ifPresent(conn -> {
            conn.setConnected(false);
            conn.setAccessToken("REVOKED");
            connectionRepository.save(conn);
            log.info("LinkedIn disconnected for user: {}", userId);
        });
    }

    /**
     * Refresh the access token if needed.
     */
    @Transactional
    public void refreshTokenIfNeeded(UUID userId) {
        connectionRepository.findByUserId(userId).ifPresent(conn -> {
            if (conn.isTokenExpired() && conn.canRefresh()) {
                try {
                    Map<String, String> tokenResponse = refreshAccessToken(conn.getRefreshToken());
                    conn.setAccessToken(tokenResponse.get("access_token"));
                    conn.setTokenExpiresAt(Instant.now().plusSeconds(
                            Long.parseLong(tokenResponse.get("expires_in"))));
                    conn.setSyncError(null);
                    connectionRepository.save(conn);
                    log.info("LinkedIn token refreshed for user: {}", userId);
                } catch (Exception e) {
                    conn.setSyncError("Token refresh failed: " + e.getMessage());
                    conn.setConnected(false);
                    connectionRepository.save(conn);
                    log.error("Failed to refresh LinkedIn token for user: {}", userId, e);
                }
            }
        });
    }

    private Map<String, String> exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=authorization_code" +
                "&code=" + code +
                "&redirect_uri=" + redirectUri +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.linkedin.com/oauth/v2/accessToken",
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to exchange code for token", e);
            throw new AuthenticationException("Failed to connect LinkedIn: " + e.getMessage());
        }
    }

    private Map<String, String> refreshAccessToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=refresh_token" +
                "&refresh_token=" + refreshToken +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://www.linkedin.com/oauth/v2/accessToken",
                HttpMethod.POST,
                request,
                Map.class
        );
        return response.getBody();
    }

    private LinkedInDTO.Profile fetchLinkedInProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/me",
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            String id = (String) body.get("id");
            String firstName = getLocalizedValue(body, "firstName");
            String lastName = getLocalizedValue(body, "lastName");

            return LinkedInDTO.Profile.builder()
                    .id(id)
                    .firstName(firstName)
                    .lastName(lastName)
                    .profileUrl("https://www.linkedin.com/in/" + id)
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch LinkedIn profile", e);
            throw new AuthenticationException("Failed to fetch LinkedIn profile: " + e.getMessage());
        }
    }

    private String getLocalizedValue(Map<String, Object> body, String field) {
        Map<String, Object> fieldData = (Map<String, Object>) body.get(field);
        if (fieldData != null) {
            Map<String, String> localized = (Map<String, String>) fieldData.get("localized");
            if (localized != null && !localized.isEmpty()) {
                return localized.values().iterator().next();
            }
        }
        return "";
    }
}
