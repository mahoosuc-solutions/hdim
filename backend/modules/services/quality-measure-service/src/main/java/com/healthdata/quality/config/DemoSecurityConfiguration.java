package com.healthdata.quality.config;

import com.healthdata.authentication.config.JwtConfig;
import com.healthdata.authentication.service.JwtTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Demo Security Configuration for Quality Measure Service
 *
 * Provides stub JwtConfig and JwtTokenService implementations for demo/test profiles
 * when running with bootRun (where test classes aren't on classpath).
 *
 * This allows the service to start without requiring a full authentication
 * infrastructure for demo purposes.
 *
 * NOTE: This should NEVER be used in production environments.
 */
@Configuration
@Profile("test")
@Slf4j
public class DemoSecurityConfiguration {

    /**
     * Demo JwtConfig that provides stub values.
     */
    @Bean
    @Primary
    public JwtConfig jwtConfig() {
        log.info("Creating Demo JwtConfig for test profile");
        JwtConfig config = new JwtConfig();
        config.setSecret("demo_jwt_secret_key_for_testing_only_minimum_256_bits_required_for_hs512_algorithm_this_is_long_enough");
        config.setAccessTokenExpiration(java.time.Duration.ofHours(1));
        config.setRefreshTokenExpiration(java.time.Duration.ofDays(1));
        config.setIssuer("healthdata-demo");
        config.setAudience("healthdata-api-demo");
        return config;
    }

    /**
     * Demo JwtTokenService for demo/test purposes.
     * Uses the demo JwtConfig to provide valid token operations.
     */
    @Bean
    @Primary
    public JwtTokenService jwtTokenService(JwtConfig jwtConfig) {
        log.info("Creating Demo JwtTokenService - all tokens will be accepted");
        return new DemoJwtTokenService(jwtConfig);
    }

    /**
     * Demo implementation of JwtTokenService that bypasses actual JWT validation.
     * All validation passes and returns demo user credentials.
     */
    @Slf4j
    public static class DemoJwtTokenService extends JwtTokenService {

        private static final java.util.UUID DEMO_USER_ID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
        private static final String DEMO_USERNAME = "demo-user";
        private static final java.util.Set<String> DEMO_TENANT_IDS = java.util.Set.of("demo-tenant");
        private static final java.util.Set<String> DEMO_ROLES = java.util.Set.of("USER", "ADMIN", "ANALYST");

        public DemoJwtTokenService(JwtConfig jwtConfig) {
            super(jwtConfig);
            log.info("DemoJwtTokenService initialized - authentication bypassed for demo mode");
        }

        @Override
        public boolean validateToken(String token) {
            // In demo mode, accept any non-null token
            if (token == null || token.isEmpty()) {
                log.debug("Demo mode: rejecting null/empty token");
                return false;
            }
            log.debug("Demo mode: accepting token as valid");
            return true;
        }

        @Override
        public boolean isTokenExpired(String token) {
            return false; // Tokens never expire in demo mode
        }

        @Override
        public String extractUsername(String token) {
            return DEMO_USERNAME;
        }

        @Override
        public java.util.UUID extractUserId(String token) {
            return DEMO_USER_ID;
        }

        @Override
        public java.util.Set<String> extractTenantIds(String token) {
            return DEMO_TENANT_IDS;
        }

        @Override
        public java.util.Set<String> extractRoles(String token) {
            return DEMO_ROLES;
        }

        @Override
        public java.util.Date getExpirationDate(String token) {
            return new java.util.Date(System.currentTimeMillis() + 86400000L);
        }

        @Override
        public String extractJwtId(String token) {
            return "demo-jwt-id";
        }

        @Override
        public java.util.Date getIssuedAt(String token) {
            return new java.util.Date();
        }
    }
}
