package com.healthdata.database.config;

import com.healthdata.database.properties.DatabaseConfigurationProperties;
import com.healthdata.database.properties.HikariProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Auto-configuration for HikariCP database connection pooling.
 *
 * Provides standardized connection pool configuration across HDIM microservices
 * based on Phase 3 HikariCP standardization patterns.
 *
 * Configuration:
 * <pre>
 * healthdata:
 *   database:
 *     enabled: true
 *     hikari:
 *       traffic-tier: MEDIUM
 * </pre>
 *
 * Activation Conditions:
 * - HikariCP is on the classpath
 * - healthdata.database.enabled=true (default)
 * - No custom DataSource bean exists
 * - Standard Spring datasource properties exist
 *
 * This configuration runs BEFORE DataSourceAutoConfiguration to provide
 * our custom HikariCP configuration when traffic-tier is set.
 */
@AutoConfiguration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@ConditionalOnClass(HikariDataSource.class)
@ConditionalOnProperty(name = "healthdata.database.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DatabaseConfigurationProperties.class)
public class DatabaseAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAutoConfiguration.class);

    /**
     * Component that logs HikariCP configuration on application startup.
     * Uses ApplicationReadyEvent to ensure logging happens after full initialization.
     */
    @Component
    @ConditionalOnProperty(name = "healthdata.database.enabled", havingValue = "true", matchIfMissing = true)
    static class ConfigurationLogger {

        private final DatabaseConfigurationProperties properties;

        ConfigurationLogger(DatabaseConfigurationProperties properties) {
            this.properties = properties;
        }

        @EventListener(ApplicationReadyEvent.class)
        public void logConfiguration() {
            HikariProperties hikari = properties.getHikari();

            log.info("╔════════════════════════════════════════════════════════════════╗");
            log.info("║  HealthData HikariCP Configuration                             ║");
            log.info("╠════════════════════════════════════════════════════════════════╣");

            if (hikari.getTrafficTier() != null) {
                log.info("║  Traffic Tier:        {:<40} ║", hikari.getTrafficTier());
                log.info("║  Pool Size:           {:<40} ║", hikari.getEffectiveMaximumPoolSize());
                log.info("║  Min Idle:            {:<40} ║", hikari.getEffectiveMinimumIdle());
            } else if (hikari.getMaximumPoolSize() != null) {
                log.info("║  Traffic Tier:        {:<40} ║", "(explicit configuration)");
                log.info("║  Pool Size:           {:<40} ║", hikari.getMaximumPoolSize());
                log.info("║  Min Idle:            {:<40} ║", hikari.getMinimumIdle());
            } else {
                log.info("║  Configuration:       {:<40} ║", "Not configured (using service defaults)");
            }

            log.info("║  Connection Timeout:  {:<40} ║", formatMs(hikari.getConnectionTimeout()));
            log.info("║  Idle Timeout:        {:<40} ║", formatMs(hikari.getIdleTimeout()));
            log.info("║  Max Lifetime:        {:<40} ║", formatMs(hikari.getMaxLifetime()));
            log.info("║  Keepalive Time:      {:<40} ║", formatMs(hikari.getKeepaliveTime()));
            log.info("║  Leak Detection:      {:<40} ║", formatMs(hikari.getLeakDetectionThreshold()));
            log.info("╚════════════════════════════════════════════════════════════════╝");
        }

        private String formatMs(long milliseconds) {
            if (milliseconds >= 60000) {
                return String.format("%dms (%d min)", milliseconds, milliseconds / 60000);
            }
            return String.format("%dms (%d sec)", milliseconds, milliseconds / 1000);
        }
    }

    /**
     * Configure HikariDataSource bean with our custom settings.
     * This runs BEFORE DataSourceAutoConfiguration, so it takes precedence.
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    @ConditionalOnProperty(name = "healthdata.database.hikari.traffic-tier")
    public HikariDataSource dataSource(
            DatabaseConfigurationProperties properties,
            DataSourceProperties dataSourceProperties) {

        HikariProperties hikari = properties.getHikari();

        HikariConfig config = new HikariConfig();

        // Standard Spring Boot datasource properties
        config.setJdbcUrl(dataSourceProperties.getUrl());
        config.setUsername(dataSourceProperties.getUsername());
        config.setPassword(dataSourceProperties.getPassword());
        if (dataSourceProperties.getDriverClassName() != null) {
            config.setDriverClassName(dataSourceProperties.getDriverClassName());
        }

        // HealthData standardized HikariCP configuration
        config.setMaximumPoolSize(hikari.getEffectiveMaximumPoolSize());
        config.setMinimumIdle(hikari.getEffectiveMinimumIdle());
        config.setConnectionTimeout(hikari.getConnectionTimeout());
        config.setIdleTimeout(hikari.getIdleTimeout());
        config.setMaxLifetime(hikari.getMaxLifetime());
        config.setKeepaliveTime(hikari.getKeepaliveTime());
        config.setLeakDetectionThreshold(hikari.getLeakDetectionThreshold());
        config.setValidationTimeout(hikari.getValidationTimeout());
        config.setAutoCommit(hikari.isAutoCommit());

        log.info("Creating HikariDataSource with {} traffic tier configuration",
                hikari.getTrafficTier());

        return new HikariDataSource(config);
    }
}
