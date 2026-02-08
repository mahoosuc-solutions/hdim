package com.healthdata.investor.service;

import com.healthdata.investor.dto.ZohoDTO;
import com.healthdata.investor.entity.InvestorUser;
import com.healthdata.investor.entity.ZohoConnection;
import com.healthdata.investor.exception.AuthenticationException;
import com.healthdata.investor.exception.ResourceNotFoundException;
import com.healthdata.investor.repository.InvestorUserRepository;
import com.healthdata.investor.repository.ZohoConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for Zoho ONE OAuth 2.0 integration.
 * Enables single sign-on for 45+ Zoho applications.
 *
 * Supported Zoho Applications:
 * - Zoho CRM (contact management)
 * - Zoho Campaigns (email marketing)
 * - Zoho Bookings (meeting scheduler)
 * - Zoho Analytics (BI/reporting)
 * - Zoho Social (social media management)
 * - Zoho Mail, Meeting, Projects, Books, and 35+ more
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ZohoOAuthService {

    private final ZohoConnectionRepository connectionRepository;
    private final InvestorUserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // In-memory state token storage (TODO: migrate to Redis for production)
    private final Map<String, String> stateTokens = new ConcurrentHashMap<>();

    @Value("${zoho.oauth2.client-id}")
    private String clientId;

    @Value("${zoho.oauth2.client-secret}")
    private String clientSecret;

    @Value("${zoho.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${zoho.oauth2.scope}")
    private String scope;

    @Value("${zoho.api.accounts-url:https://accounts.zoho.com}")
    private String accountsUrl;

    @Value("${zoho.api.enabled:false}")
    private boolean enabled;

    /**
     * Generate Zoho OAuth authorization URL
     *
     * @param state CSRF protection token
     * @return Authorization URL for user to visit
     */
    public String getAuthorizationUrl(String state) {
        if (!enabled) {
            throw new AuthenticationException("Zoho API is not enabled");
        }

        if (clientId == null || clientId.isEmpty()) {
            throw new AuthenticationException("Zoho client ID not configured");
        }

        // Store state token for CSRF validation
        stateTokens.put(state, state);

        return UriComponentsBuilder.fromHttpUrl(accountsUrl + "/oauth/v2/auth")
                .queryParam("scope", scope)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("access_type", "offline") // Request refresh token
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    /**
     * Handle OAuth callback and exchange authorization code for access token
     *
     * @param code OAuth authorization code
     * @param state CSRF protection token
     * @param accountsServer Zoho data center domain (zoho.com, zoho.eu, etc.)
     * @param userId User ID to associate connection with
     * @return Connection status
     */
    @Transactional
    public ZohoDTO.ConnectionStatus handleOAuthCallback(
            String code,
            String state,
            String accountsServer,
            UUID userId
    ) {
        // Validate state token (CSRF protection)
        if (!stateTokens.containsKey(state)) {
            throw new AuthenticationException("Invalid state token - possible CSRF attack");
        }
        stateTokens.remove(state);

        // Find user
        InvestorUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        // Determine API domain from accounts server
        String apiDomain = accountsServer != null ? accountsServer : "zoho.com";
        String tokenUrl = "https://accounts." + apiDomain + "/oauth/v2/token";

        // Exchange code for access + refresh tokens
        Map<String, Object> tokenResponse = exchangeCodeForToken(code, tokenUrl);

        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Integer expiresIn = (Integer) tokenResponse.get("expires_in");

        // Fetch Zoho user profile
        ZohoDTO.Profile profile = fetchZohoProfile(accessToken, apiDomain);

        // Save or update connection
        ZohoConnection connection = connectionRepository.findByUserId(userId)
                .orElse(ZohoConnection.builder()
                        .user(user)
                        .tenantId(user.getTenantId())
                        .build());

        connection.setAccessToken(accessToken);
        connection.setRefreshToken(refreshToken);
        connection.setApiDomain(apiDomain);
        connection.setOrganizationId(profile.getOrganizationId());
        connection.setZohoEmail(profile.getEmail());
        connection.setDisplayName(profile.getFirstName() + " " + profile.getLastName());
        connection.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
        connection.setScope(scope);
        connection.setConnected(true);
        connection.setLastSync(Instant.now());
        connection.setSyncError(null);

        connectionRepository.save(connection);
        log.info("Zoho ONE connected for user: {} with organization: {}",
                user.getEmail(), profile.getOrganizationId());

        return ZohoDTO.ConnectionStatus.builder()
                .connected(true)
                .zohoEmail(profile.getEmail())
                .displayName(connection.getDisplayName())
                .organizationId(profile.getOrganizationId())
                .apiDomain(apiDomain)
                .expiresAt(connection.getTokenExpiresAt())
                .lastSync(connection.getLastSync())
                .build();
    }

    /**
     * Get connection status for user
     *
     * @param userId User ID
     * @return Connection status
     */
    public ZohoDTO.ConnectionStatus getConnectionStatus(UUID userId) {
        return connectionRepository.findByUserId(userId)
                .map(conn -> ZohoDTO.ConnectionStatus.builder()
                        .connected(conn.isConnected())
                        .zohoEmail(conn.getZohoEmail())
                        .displayName(conn.getDisplayName())
                        .organizationId(conn.getOrganizationId())
                        .apiDomain(conn.getApiDomain())
                        .expiresAt(conn.getTokenExpiresAt())
                        .lastSync(conn.getLastSync())
                        .syncError(conn.getSyncError())
                        .build())
                .orElse(ZohoDTO.ConnectionStatus.builder()
                        .connected(false)
                        .build());
    }

    /**
     * Disconnect Zoho account
     *
     * @param userId User ID
     */
    @Transactional
    public void disconnect(UUID userId) {
        connectionRepository.findByUserId(userId).ifPresent(conn -> {
            conn.setConnected(false);
            conn.setAccessToken("REVOKED");
            conn.setRefreshToken("REVOKED");
            connectionRepository.save(conn);
            log.info("Zoho ONE disconnected for user: {}", userId);
        });
    }

    /**
     * Refresh access token if needed
     *
     * @param userId User ID
     */
    @Transactional
    public void refreshTokenIfNeeded(UUID userId) {
        connectionRepository.findByUserId(userId).ifPresent(conn -> {
            if (conn.needsRefresh() && conn.canRefresh()) {
                try {
                    String tokenUrl = "https://accounts." + conn.getApiDomain() + "/oauth/v2/token";
                    Map<String, Object> tokenResponse = refreshAccessToken(conn.getRefreshToken(), tokenUrl);

                    String accessToken = (String) tokenResponse.get("access_token");
                    Integer expiresIn = (Integer) tokenResponse.get("expires_in");

                    conn.setAccessToken(accessToken);
                    conn.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
                    conn.setSyncError(null);
                    connectionRepository.save(conn);
                    log.info("Zoho token refreshed for user: {}", userId);
                } catch (Exception e) {
                    conn.setSyncError("Token refresh failed: " + e.getMessage());
                    conn.setConnected(false);
                    connectionRepository.save(conn);
                    log.error("Failed to refresh Zoho token for user: {}", userId, e);
                }
            }
        });
    }

    /**
     * Get active access token for user (refreshes if needed)
     *
     * @param userId User ID
     * @return Access token
     */
    public String getAccessToken(UUID userId) {
        ZohoConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthenticationException("No Zoho connection found for user"));

        if (!connection.isConnected()) {
            throw new AuthenticationException("Zoho connection is not active");
        }

        if (connection.needsRefresh()) {
            refreshTokenIfNeeded(userId);
            connection = connectionRepository.findByUserId(userId)
                    .orElseThrow(() -> new AuthenticationException("Connection lost during refresh"));
        }

        return connection.getAccessToken();
    }

    /**
     * Get API domain for user
     *
     * @param userId User ID
     * @return API domain (zoho.com, zoho.eu, etc.)
     */
    public String getApiDomain(UUID userId) {
        return connectionRepository.findByUserId(userId)
                .map(ZohoConnection::getApiDomain)
                .orElse("zoho.com");
    }

    private Map<String, Object> exchangeCodeForToken(String code, String tokenUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to exchange code for token", e);
            throw new AuthenticationException("Failed to connect Zoho: " + e.getMessage());
        }
    }

    private Map<String, Object> refreshAccessToken(String refreshToken, String tokenUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("refresh_token", refreshToken);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                Map.class
        );
        return response.getBody();
    }

    private ZohoDTO.Profile fetchZohoProfile(String accessToken, String apiDomain) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            String profileUrl = "https://accounts." + apiDomain + "/oauth/user/info";
            ResponseEntity<Map> response = restTemplate.exchange(
                    profileUrl,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            return ZohoDTO.Profile.builder()
                    .email((String) body.get("Email"))
                    .firstName((String) body.get("First_Name"))
                    .lastName((String) body.get("Last_Name"))
                    .organizationId((String) body.get("ZGID"))
                    .timezone((String) body.get("time_zone"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch Zoho profile", e);
            throw new AuthenticationException("Failed to fetch Zoho profile: " + e.getMessage());
        }
    }
}
