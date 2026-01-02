package com.healthdata.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class JwtTokenServiceTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SecurityAutoConfiguration.class)
            .withPropertyValues(
                    "healthdata.security.jwt.secret=0123456789ABCDEF0123456789ABCDEF",
                    "healthdata.security.jwt.expiration=PT15M");

    @Test
    void shouldIssueAndParseToken() {
        contextRunner.run(context -> {
            JwtTokenService tokenService = context.getBean(JwtTokenService.class);

            String token = tokenService.issueToken("user-123", Duration.ofMinutes(5));
            JwtTokenService.JwtClaims claims = tokenService.parse(token);

            assertThat(claims.subject()).isEqualTo("user-123");
            assertThat(claims.expiresAt()).isAfter(claims.issuedAt());
        });
    }

    @Test
    void shouldRejectExpiredTokens() {
        contextRunner.run(context -> {
            JwtTokenService tokenService = context.getBean(JwtTokenService.class);

            String token = tokenService.issueToken("user-123", Duration.ofSeconds(1));
            // Force expiration by overriding system clock via explicit validation duration
            assertThatThrownBy(() -> tokenService.parse(token, Duration.ofSeconds(-5)))
                    .isInstanceOf(JwtValidationException.class)
                    .hasMessageContaining("expired");
        });
    }
}
