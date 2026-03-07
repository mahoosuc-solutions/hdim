package com.healthdata.hedisadapter.integration;

import com.healthdata.hedisadapter.config.HedisProperties;
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
 * Validates that the hedis adapter is dormant when HEDIS_ENABLED=false.
 * When disabled, the WebSocket bridge and REST clients should NOT be created.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "external.hedis.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:hedis_toggle_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=localhost:0",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@Tag("integration")
@DisplayName("Feature Toggle: hedis adapter disabled")
class HedisFeatureToggleTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private HedisProperties properties;

    @Test
    @DisplayName("hedis should be disabled when HEDIS_ENABLED=false")
    void hedisDisabled_propertyShouldBeFalse() {
        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("hedis RestTemplate bean should NOT exist when disabled")
    void hedisDisabled_restTemplateShouldNotExist() {
        assertThat(context.containsBean("hedisRestTemplate")).isFalse();
    }

    @Test
    @DisplayName("hedis CQL RestTemplate bean should NOT exist when disabled")
    void hedisDisabled_cqlRestTemplateShouldNotExist() {
        assertThat(context.containsBean("hedisCqlRestTemplate")).isFalse();
    }

    @Test
    @DisplayName("KafkaToWebSocketBridge should NOT exist when disabled")
    void hedisDisabled_webSocketBridgeShouldNotExist() {
        assertThat(context.containsBean("kafkaToWebSocketBridge")).isFalse();
    }
}
