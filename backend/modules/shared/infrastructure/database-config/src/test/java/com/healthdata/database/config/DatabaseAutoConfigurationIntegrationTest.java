package com.healthdata.database.config;

import com.healthdata.database.properties.DatabaseConfigurationProperties;
import com.healthdata.database.properties.HikariProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DatabaseAutoConfiguration.class,
        DataSourceProperties.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "healthdata.database.enabled=true",
        "healthdata.database.hikari.traffic-tier=MEDIUM"
})
@DisplayName("DatabaseAutoConfiguration Integration Tests")
class DatabaseAutoConfigurationIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DatabaseConfigurationProperties properties;

    @Test
    @DisplayName("Should create DatabaseConfigurationProperties bean")
    void shouldCreateDatabaseConfigurationPropertiesBean() {
        assertThat(properties).isNotNull();
        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should bind healthdata.database properties correctly")
    void shouldBindPropertiesCorrectly() {
        assertThat(properties.isEnabled()).isTrue();

        HikariProperties hikari = properties.getHikari();
        assertThat(hikari.getTrafficTier()).isEqualTo(TrafficTier.MEDIUM);
        assertThat(hikari.getEffectiveMaximumPoolSize()).isEqualTo(20);
        assertThat(hikari.getEffectiveMinimumIdle()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should create HikariDataSource bean when traffic-tier configured")
    void shouldCreateHikariDataSourceBean() {
        assertThat(context.containsBean("dataSource")).isTrue();

        DataSource dataSource = context.getBean(DataSource.class);
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);

        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(20);
        assertThat(hikariDataSource.getMinimumIdle()).isEqualTo(5);
        assertThat(hikariDataSource.getConnectionTimeout()).isEqualTo(20000);
        assertThat(hikariDataSource.getIdleTimeout()).isEqualTo(300000);
        assertThat(hikariDataSource.getMaxLifetime()).isEqualTo(1800000);
        assertThat(hikariDataSource.getKeepaliveTime()).isEqualTo(240000);
        assertThat(hikariDataSource.getLeakDetectionThreshold()).isEqualTo(60000);
        assertThat(hikariDataSource.getValidationTimeout()).isEqualTo(5000);
        assertThat(hikariDataSource.isAutoCommit()).isTrue();
    }

    @Test
    @DisplayName("Should configure DataSource with correct JDBC URL")
    void shouldConfigureDataSourceWithCorrectJdbcUrl() {
        HikariDataSource hikariDataSource = context.getBean(HikariDataSource.class);
        assertThat(hikariDataSource.getJdbcUrl()).isEqualTo("jdbc:h2:mem:testdb");
        assertThat(hikariDataSource.getUsername()).isEqualTo("sa");
    }

    @Test
    @DisplayName("Should verify 6x safety margin is applied")
    void shouldVerify6xSafetyMargin() {
        HikariDataSource hikariDataSource = context.getBean(HikariDataSource.class);
        long maxLifetime = hikariDataSource.getMaxLifetime();
        long idleTimeout = hikariDataSource.getIdleTimeout();

        assertThat(maxLifetime).isEqualTo(idleTimeout * 6);
    }

    @Test
    @DisplayName("Should verify keepalive is less than idle timeout")
    void shouldVerifyKeepaliveIsLessThanIdleTimeout() {
        HikariDataSource hikariDataSource = context.getBean(HikariDataSource.class);
        long keepaliveTime = hikariDataSource.getKeepaliveTime();
        long idleTimeout = hikariDataSource.getIdleTimeout();

        assertThat(keepaliveTime).isLessThan(idleTimeout);
    }
}
