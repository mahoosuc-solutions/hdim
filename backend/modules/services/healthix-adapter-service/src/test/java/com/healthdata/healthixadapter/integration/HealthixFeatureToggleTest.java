package com.healthdata.healthixadapter.integration;

import com.healthdata.healthixadapter.config.HealthixProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Healthix adapter is dormant when HEALTHIX_ENABLED=false.
 * When disabled, no REST clients or mTLS contexts should be created.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "external.healthix.enabled=false",
        "external.healthix.mtls.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:healthix_toggle_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=localhost:0",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@Tag("integration")
@DisplayName("Feature Toggle: Healthix adapter disabled")
class HealthixFeatureToggleTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private HealthixProperties properties;

    @Test
    @DisplayName("Healthix should be disabled when HEALTHIX_ENABLED=false")
    void healthixDisabled_propertyShouldBeFalse() {
        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("FHIR RestTemplate bean should NOT exist when disabled")
    void healthixDisabled_fhirRestTemplateShouldNotExist() {
        assertThat(context.containsBean("healthixFhirRestTemplate")).isFalse();
    }

    @Test
    @DisplayName("MPI RestTemplate bean should NOT exist when disabled")
    void healthixDisabled_mpiRestTemplateShouldNotExist() {
        assertThat(context.containsBean("healthixMpiRestTemplate")).isFalse();
    }

    @Test
    @DisplayName("mTLS SSLContext bean should NOT exist when disabled")
    void healthixDisabled_mtlsSslContextShouldNotExist() {
        assertThat(context.containsBean("healthixSslContext")).isFalse();
    }
}
