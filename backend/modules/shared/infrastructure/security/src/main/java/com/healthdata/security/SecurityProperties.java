package com.healthdata.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for shared security components.
 */
@Validated
@ConfigurationProperties(prefix = "healthdata.security")
public class SecurityProperties {

    private final JwtProperties jwt = new JwtProperties();

    public JwtProperties getJwt() {
        return jwt;
    }

    public static class JwtProperties {

        /**
         * Secret used to sign JWT tokens (HS256).
         */
        @NotBlank
        private String secret;

        /**
         * Token expiration duration. Defaults to 15 minutes if not provided.
         */
        @NotNull
        private Duration expiration = Duration.ofMinutes(15);

        /**
         * Issuer name embedded in tokens for traceability.
         */
        @NotBlank
        private String issuer = "healthdata-in-motion";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getExpiration() {
            return expiration;
        }

        public void setExpiration(Duration expiration) {
            this.expiration = expiration;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }
}
