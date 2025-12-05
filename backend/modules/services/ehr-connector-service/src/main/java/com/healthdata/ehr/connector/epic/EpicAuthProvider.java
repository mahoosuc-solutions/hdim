package com.healthdata.ehr.connector.epic;

import com.healthdata.ehr.connector.core.AuthProvider;
import com.healthdata.ehr.connector.core.EhrConnectionException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Epic OAuth2/JWT Backend Services authentication provider.
 * Implements Epic's JWT-based authentication using RS384 signing algorithm.
 */
@Component
public class EpicAuthProvider implements AuthProvider {

    private static final Logger logger = LoggerFactory.getLogger(EpicAuthProvider.class);
    private static final String GRANT_TYPE = "client_credentials";
    private static final int JWT_EXPIRATION_MINUTES = 5;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final EpicConnectionConfig config;
    private final RestTemplate restTemplate;
    private EpicTokenResponse cachedToken;

    public EpicAuthProvider(EpicConnectionConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getAccessToken() {
        if (isTokenValid()) {
            logger.debug("Using cached access token");
            return cachedToken.getAccessToken();
        }

        logger.info("Obtaining new access token from Epic");
        return obtainNewToken();
    }

    @Override
    public String refreshToken() {
        logger.info("Refreshing access token");
        invalidateToken();
        return obtainNewToken();
    }

    @Override
    public boolean isTokenValid() {
        return cachedToken != null && !cachedToken.isExpired();
    }

    @Override
    public void invalidateToken() {
        logger.debug("Invalidating cached token");
        cachedToken = null;
    }

    /**
     * Create JWT assertion for Epic Backend Services authentication.
     * Uses RS384 algorithm as required by Epic.
     *
     * @return signed JWT assertion
     */
    public String createJwtAssertion() {
        if (config.getPrivateKey() == null) {
            throw new EhrConnectionException("Private key is not configured", "Epic");
        }

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(JWT_EXPIRATION_MINUTES * 60);

        try {
            return Jwts.builder()
                    .setIssuer(config.getClientId())
                    .setSubject(config.getClientId())
                    .setAudience(config.getTokenUrl())
                    .setId(UUID.randomUUID().toString())
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(expiration))
                    .signWith(config.getPrivateKey(), SignatureAlgorithm.RS384)
                    .compact();
        } catch (Exception e) {
            throw new EhrConnectionException("Failed to create JWT assertion", "Epic", e);
        }
    }

    private String obtainNewToken() {
        String jwtAssertion = createJwtAssertion();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", GRANT_TYPE);
        body.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        body.add("client_assertion", jwtAssertion);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                ResponseEntity<EpicTokenResponse> response = restTemplate.exchange(
                        config.getTokenUrl(),
                        HttpMethod.POST,
                        request,
                        EpicTokenResponse.class
                );

                if (response.getBody() == null) {
                    throw new EhrConnectionException("Token response body is null", "Epic");
                }

                cachedToken = response.getBody();
                logger.info("Successfully obtained access token, expires in {} seconds",
                        cachedToken.getExpiresIn());
                return cachedToken.getAccessToken();

            } catch (HttpClientErrorException e) {
                lastException = e;

                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    attempt++;
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        logger.warn("Rate limit exceeded, retrying in {} ms (attempt {}/{})",
                                RETRY_DELAY_MS, attempt, MAX_RETRY_ATTEMPTS);
                        try {
                            Thread.sleep(RETRY_DELAY_MS * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new EhrConnectionException("Interrupted while waiting to retry", "Epic", ie);
                        }
                        continue;
                    }
                }

                logger.error("Failed to obtain access token: {} - {}",
                        e.getStatusCode(), e.getResponseBodyAsString());
                throw new EhrConnectionException(
                        "Failed to obtain access token from Epic: " + e.getMessage(),
                        "Epic",
                        e.getStatusCode().value(),
                        e
                );
            } catch (Exception e) {
                lastException = e;
                logger.error("Unexpected error obtaining access token", e);
                throw new EhrConnectionException(
                        "Failed to obtain access token from Epic: " + e.getMessage(),
                        "Epic",
                        e
                );
            }
        }

        throw new EhrConnectionException(
                "Failed to obtain access token after " + MAX_RETRY_ATTEMPTS + " attempts",
                "Epic",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                lastException
        );
    }
}
