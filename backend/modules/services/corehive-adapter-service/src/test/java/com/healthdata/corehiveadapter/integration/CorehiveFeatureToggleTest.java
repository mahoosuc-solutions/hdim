package com.healthdata.corehiveadapter.integration;

import com.healthdata.corehiveadapter.config.CorehiveProperties;
import com.healthdata.corehiveadapter.service.CorehiveAdapterService;
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
 * Validates that the CoreHive adapter is dormant when COREHIVE_ENABLED=false.
 * The adapter service bean should still exist (it's always registered),
 * but the REST client and circuit breaker beans should NOT be created.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "external.corehive.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:corehive_toggle_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=localhost:0",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@Tag("integration")
@DisplayName("Feature Toggle: CoreHive adapter disabled")
class CorehiveFeatureToggleTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CorehiveProperties properties;

    @Test
    @DisplayName("CoreHive should be disabled when COREHIVE_ENABLED=false")
    void corehiveDisabled_propertyShouldBeFalse() {
        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("CorehiveRestTemplate bean should NOT exist when disabled")
    void corehiveDisabled_restTemplateShouldNotExist() {
        assertThat(context.containsBean("corehiveRestTemplate")).isFalse();
    }

    @Test
    @DisplayName("CircuitBreakerRegistry bean should NOT exist when disabled")
    void corehiveDisabled_circuitBreakerShouldNotExist() {
        assertThat(context.containsBean("corehiveCircuitBreakerRegistry")).isFalse();
    }
}
