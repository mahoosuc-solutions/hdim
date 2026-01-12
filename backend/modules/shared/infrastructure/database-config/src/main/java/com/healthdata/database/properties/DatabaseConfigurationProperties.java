package com.healthdata.database.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Top-level database configuration properties.
 *
 * Configuration Prefix: healthdata.database
 *
 * Example:
 * <pre>
 * healthdata:
 *   database:
 *     enabled: true
 *     hikari:
 *       traffic-tier: MEDIUM
 * </pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "healthdata.database")
public class DatabaseConfigurationProperties {

    /**
     * Enable database auto-configuration.
     * Default: true (can be disabled for services that need custom config)
     */
    private boolean enabled = true;

    /**
     * HikariCP connection pool configuration.
     */
    @NestedConfigurationProperty
    private HikariProperties hikari = new HikariProperties();
}
