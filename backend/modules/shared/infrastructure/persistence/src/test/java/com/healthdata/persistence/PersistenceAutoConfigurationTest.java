package com.healthdata.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.AuditorAware;

class PersistenceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceAutoConfiguration.class))
            .withPropertyValues(
                    "healthdata.persistence.primary.url=jdbc:h2:mem:primary;DB_CLOSE_DELAY=-1",
                    "healthdata.persistence.primary.username=sa",
                    "healthdata.persistence.primary.password=",
                    "healthdata.persistence.primary.driver-class-name=org.h2.Driver",
                    "healthdata.persistence.primary.pool-name=primary-pool",
                    "healthdata.persistence.jpa.auditing-enabled=false",
                    "healthdata.persistence.jpa.properties.hibernate.show_sql=false");

    @Test
    void shouldCreateHikariDataSourceWithConfiguredPoolName() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DataSource.class);
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(dataSource).isInstanceOf(HikariDataSource.class);

            HikariDataSource hikari = (HikariDataSource) dataSource;
            assertThat(hikari.getPoolName()).isEqualTo("primary-pool");
            assertThat(hikari.getJdbcUrl()).contains("jdbc:h2:mem:primary");
        });
    }

    @Test
    void shouldExposeDefaultAuditorAware() {
        contextRunner.run(context -> {
            @SuppressWarnings("unchecked")
            AuditorAware<String> auditorAware = context.getBean(AuditorAware.class);
            Optional<String> auditor = auditorAware.getCurrentAuditor();

            assertThat(auditor).isPresent();
            assertThat(auditor.orElseThrow()).isEqualTo("system@healthdata");
        });
    }
}
