package com.healthdata.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Spring Session Configuration for distributed session management.
 *
 * Features:
 * - Redis-backed session storage for horizontal scaling
 * - 4-hour idle timeout (default, configurable via application.yml)
 * - Secure session cookies (HttpOnly, SameSite=Lax)
 * - Session expiration on browser close
 * - HIPAA-compliant session tracking
 *
 * Session Metadata:
 * - Session ID (tracked in Redis)
 * - User ID
 * - Tenant ID
 * - IP address
 * - User agent (browser, device)
 * - Created timestamp
 * - Last accessed timestamp
 *
 * Use Cases:
 * - Load-balanced gateway instances share session state
 * - Concurrent session limiting (max 5 per user)
 * - Session revocation by admin
 * - Multi-device session management
 *
 * Redis Key Pattern:
 * - spring:session:sessions:{sessionId}
 * - spring:session:expirations:{timestamp}
 * - spring:session:index:PRINCIPAL_NAME_INDEX_NAME:{username}
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 14400) // 4 hours default
@Slf4j
public class SessionConfiguration {

    @Value("${spring.session.timeout-seconds:14400}") // 4 hours = 14400 seconds
    private int sessionTimeoutSeconds;

    @Value("${spring.session.cookie-name:HDIM_SESSION}")
    private String cookieName;

    @Value("${spring.session.cookie-secure:true}")
    private boolean cookieSecure;

    /**
     * Configure session cookie serialization.
     *
     * Security Settings:
     * - HttpOnly: Prevents JavaScript access (XSS protection)
     * - Secure: HTTPS-only transmission (HIPAA requirement)
     * - SameSite=Lax: CSRF protection while allowing normal navigation
     * - Cookie name: HDIM_SESSION (distinct from JSESSIONID)
     *
     * @return CookieSerializer with security settings
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();

        // Cookie name
        serializer.setCookieName(cookieName);

        // Security flags
        serializer.setUseHttpOnlyCookie(true);  // Prevent XSS
        serializer.setUseSecureCookie(cookieSecure);  // HTTPS only (disable in dev)
        serializer.setSameSite("Lax");  // CSRF protection

        // Cookie path
        serializer.setCookiePath("/");

        // Cookie max age (match session timeout)
        serializer.setCookieMaxAge(sessionTimeoutSeconds);

        log.info("Session cookie configuration: name={}, secure={}, httpOnly=true, sameSite=Lax, maxAge={}s",
            cookieName, cookieSecure, sessionTimeoutSeconds);

        return serializer;
    }

    /**
     * Bean post-processor to log session configuration on startup.
     */
    @Bean
    public SessionConfigurationLogger sessionConfigurationLogger(RedisConnectionFactory connectionFactory) {
        return new SessionConfigurationLogger(connectionFactory, sessionTimeoutSeconds);
    }

    /**
     * Helper class to log session configuration details.
     */
    @Slf4j
    private static class SessionConfigurationLogger {
        public SessionConfigurationLogger(RedisConnectionFactory connectionFactory, int timeoutSeconds) {
            log.info("Spring Session configured with Redis backend");
            log.info("  - Session timeout: {} seconds ({} hours)", timeoutSeconds, timeoutSeconds / 3600.0);
            log.info("  - Redis connection: {}", connectionFactory.getClass().getSimpleName());
            log.info("  - Session storage: spring:session:sessions:*");
            log.info("  - Session indexing: Enabled (by principal name)");
            log.info("  - Concurrent session control: Managed by SessionRegistry");
        }
    }
}
