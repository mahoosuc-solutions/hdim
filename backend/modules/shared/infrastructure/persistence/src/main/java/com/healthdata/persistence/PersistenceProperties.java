package com.healthdata.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties backing the shared persistence infrastructure.
 */
@Validated
@ConfigurationProperties(prefix = "healthdata.persistence")
public class PersistenceProperties {

    private final PrimaryDataSourceProperties primary = new PrimaryDataSourceProperties();
    private final JpaSettings jpa = new JpaSettings();

    public PrimaryDataSourceProperties getPrimary() {
        return primary;
    }

    public JpaSettings getJpa() {
        return jpa;
    }

    public static class PrimaryDataSourceProperties {
        /**
         * JDBC connection URL.
         */
        @NotBlank
        private String url = "jdbc:postgresql://localhost:5435/healthdata";

        /**
         * Database username.
         */
        @NotBlank
        private String username = "postgres";

        /**
         * Database password.
         */
        private String password = "postgres";

        /**
         * Optional driver class name override.
         */
        private String driverClassName = "org.postgresql.Driver";

        /**
         * HikariCP pool name.
         */
        @NotBlank
        private String poolName = "healthdata-primary";

        /**
         * Maximum pool size.
         */
        @Min(1)
        private int maximumPoolSize = 10;

        /**
         * Minimum number of idle connections retained.
         */
        @Min(0)
        private int minimumIdle = 2;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }
    }

    public static class JpaSettings {
        /**
         * Additional Hibernate properties to feed into the EntityManagerFactory.
         */
        private final Map<String, String> properties = new LinkedHashMap<>();

        /**
         * Whether to auto-enable Spring Data JPA auditing.
         */
        private boolean auditingEnabled = true;

        public Map<String, String> getProperties() {
            return properties;
        }

        public boolean isAuditingEnabled() {
            return auditingEnabled;
        }

        public void setAuditingEnabled(boolean auditingEnabled) {
            this.auditingEnabled = auditingEnabled;
        }
    }
}
