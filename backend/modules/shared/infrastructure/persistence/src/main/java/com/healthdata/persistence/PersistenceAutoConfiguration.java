package com.healthdata.persistence;

import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import com.healthdata.persistence.tenant.TenantConnectionPreparer;
import com.healthdata.persistence.tenant.TenantContext;
import com.healthdata.persistence.tenant.TenantFilter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Auto-configuration for shared persistence components (DataSource, auditing helpers).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PersistenceProperties.class)
@ConditionalOnClass({ DataSource.class, EnableJpaAuditing.class })
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@ComponentScan(basePackages = "com.healthdata.persistence.tenant")
public class PersistenceAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public DataSource primaryDataSource(PersistenceProperties properties) {
        PersistenceProperties.PrimaryDataSourceProperties primary = properties.getPrimary();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(primary.getUrl());
        config.setUsername(primary.getUsername());
        config.setPassword(primary.getPassword());
        config.setPoolName(primary.getPoolName());
        config.setMaximumPoolSize(primary.getMaximumPoolSize());
        config.setMinimumIdle(primary.getMinimumIdle());
        config.setAutoCommit(false);
        config.setInitializationFailTimeout(-1);

        if (primary.getDriverClassName() != null && !primary.getDriverClassName().isBlank()) {
            config.setDriverClassName(primary.getDriverClassName());
        }

        return new HikariDataSource(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditorAware<String> auditorAware() {
        // Use tenant-aware auditing - include tenant context if available
        return () -> {
            String tenant = TenantContext.getCurrentTenant();
            return Optional.of(tenant != null ? tenant + "@system" : "system@healthdata");
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "healthdata.persistence", name = "rls-enabled", havingValue = "true", matchIfMissing = true)
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "healthdata.persistence", name = "rls-enabled", havingValue = "true", matchIfMissing = true)
    public TenantConnectionPreparer tenantConnectionPreparer(DataSource dataSource) {
        return new TenantConnectionPreparer(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public HibernatePropertiesCustomizer healthdataHibernateCustomizer(PersistenceProperties properties) {
        return hibernateProperties -> mergeJpaProperties(hibernateProperties, properties.getJpa().getProperties());
    }

    private void mergeJpaProperties(Map<String, Object> target, Map<String, String> additions) {
        if (additions == null || additions.isEmpty()) {
            return;
        }
        additions.forEach((key, value) -> {
            if (!target.containsKey(key)) {
                target.put(key, value);
            }
        });
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "healthdata.persistence.jpa", name = "auditing-enabled", havingValue = "true", matchIfMissing = true)
    @EnableJpaAuditing
    static class JpaAuditingConfiguration {
        // Activation class to enable auditing when desired.
    }
}
