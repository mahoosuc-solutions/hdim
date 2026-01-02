package com.healthdata.eventrouter.integration;

import com.healthdata.eventrouter.entity.RoutingRuleEntity;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import com.healthdata.eventrouter.persistence.RoutingRuleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for multi-tenant routing rule persistence and isolation.
 * Tests focus on database-level tenant isolation for routing rules.
 *
 * Note: Full end-to-end routing tests with Kafka require external infrastructure
 * and are tested in separate E2E test suites.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Multi-Tenant Routing Integration Tests")
class MultiTenantRoutingIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure H2 with legacy mode for Hibernate 6.x compatibility
        registry.add("spring.datasource.url", () ->
            "jdbc:tc:postgresql:15-alpine:///testdb");
        registry.add("spring.jpa.properties.hibernate.dialect", () ->
            "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private RoutingRuleRepository ruleRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        ruleRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    @DisplayName("Should isolate routing rules by tenant")
    void shouldIsolateRulesByTenant() {
        // Given
        RoutingRuleEntity tenant1Rule = createRule("tenant1", "rule1", "fhir.patient.created", "tenant1.processing");
        RoutingRuleEntity tenant2Rule = createRule("tenant2", "rule2", "fhir.patient.created", "tenant2.processing");
        ruleRepository.saveAndFlush(tenant1Rule);
        ruleRepository.saveAndFlush(tenant2Rule);
        entityManager.clear();

        // When
        var rules1 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created");
        var rules2 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant2", "fhir.patient.created");

        // Then
        assertThat(rules1).hasSize(1);
        assertThat(rules1.get(0).getTargetTopic()).isEqualTo("tenant1.processing");
        assertThat(rules2).hasSize(1);
        assertThat(rules2.get(0).getTargetTopic()).isEqualTo("tenant2.processing");
    }

    @Test
    @DisplayName("Should not return rules for wrong tenant")
    void shouldNotReturnCrossTenantRules() {
        // Given
        RoutingRuleEntity tenant1Rule = createRule("tenant1", "rule1", "fhir.patient.created", "tenant1.processing");
        ruleRepository.saveAndFlush(tenant1Rule);
        entityManager.clear();

        // When
        var rules = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant2", "fhir.patient.created");

        // Then
        assertThat(rules).isEmpty();
    }

    @Test
    @DisplayName("Should support different priorities per tenant")
    void shouldSupportDifferentPrioritiesPerTenant() {
        // Given
        RoutingRuleEntity tenant1Rule = createRule("tenant1", "rule1", "fhir.patient.created", "tenant1.processing");
        tenant1Rule.setPriority(Priority.CRITICAL);

        RoutingRuleEntity tenant2Rule = createRule("tenant2", "rule2", "fhir.patient.created", "tenant2.processing");
        tenant2Rule.setPriority(Priority.LOW);

        ruleRepository.saveAndFlush(tenant1Rule);
        ruleRepository.saveAndFlush(tenant2Rule);
        entityManager.clear();

        // When
        var rules1 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created");
        var rules2 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant2", "fhir.patient.created");

        // Then
        assertThat(rules1.get(0).getPriority()).isEqualTo(Priority.CRITICAL);
        assertThat(rules2.get(0).getPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    @DisplayName("Should support tenant-specific filter expressions")
    void shouldSupportTenantSpecificFilters() {
        // Given
        RoutingRuleEntity tenant1Rule = createRule("tenant1", "rule1", "fhir.patient.created", "tenant1.processing");
        tenant1Rule.setFilterExpression("{\"region\": \"US\"}");

        RoutingRuleEntity tenant2Rule = createRule("tenant2", "rule2", "fhir.patient.created", "tenant2.processing");
        tenant2Rule.setFilterExpression("{\"region\": \"EU\"}");

        ruleRepository.saveAndFlush(tenant1Rule);
        ruleRepository.saveAndFlush(tenant2Rule);
        entityManager.clear();

        // When
        var rules1 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created");
        var rules2 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant2", "fhir.patient.created");

        // Then
        assertThat(rules1.get(0).getFilterExpression()).contains("US");
        assertThat(rules2.get(0).getFilterExpression()).contains("EU");
    }

    @Test
    @DisplayName("Should support tenant-specific transformations")
    void shouldSupportTenantSpecificTransformations() {
        // Given
        RoutingRuleEntity tenant1Rule = createRule("tenant1", "rule1", "fhir.patient.created", "tenant1.processing");
        tenant1Rule.setTransformationScript("enrichment:add-us-fields");

        RoutingRuleEntity tenant2Rule = createRule("tenant2", "rule2", "fhir.patient.created", "tenant2.processing");
        tenant2Rule.setTransformationScript("enrichment:add-eu-fields");

        ruleRepository.saveAndFlush(tenant1Rule);
        ruleRepository.saveAndFlush(tenant2Rule);
        entityManager.clear();

        // When
        var rules1 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created");
        var rules2 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant2", "fhir.patient.created");

        // Then
        assertThat(rules1.get(0).getTransformationScript()).contains("us-fields");
        assertThat(rules2.get(0).getTransformationScript()).contains("eu-fields");
    }

    @Test
    @DisplayName("Should handle multiple rules per tenant")
    void shouldHandleMultipleRulesPerTenant() {
        // Given
        RoutingRuleEntity rule1 = createRule("tenant1", "rule1", "fhir.patient.created", "processing.high-priority");
        rule1.setPriority(Priority.HIGH);
        rule1.setFilterExpression("{\"urgent\": true}");

        RoutingRuleEntity rule2 = createRule("tenant1", "rule2", "fhir.patient.created", "processing.normal");
        rule2.setPriority(Priority.MEDIUM);

        ruleRepository.saveAndFlush(rule1);
        ruleRepository.saveAndFlush(rule2);
        entityManager.clear();

        // When
        var rules = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created");

        // Then
        assertThat(rules).hasSize(2);
    }

    @Test
    @DisplayName("Should only return enabled rules")
    void shouldOnlyReturnEnabledRules() {
        // Given
        RoutingRuleEntity enabledRule = createRule("tenant1", "enabled-rule", "fhir.patient.created", "enabled.topic");
        enabledRule.setEnabled(true);

        RoutingRuleEntity disabledRule = createRule("tenant1", "disabled-rule", "fhir.patient.created", "disabled.topic");
        disabledRule.setEnabled(false);

        ruleRepository.saveAndFlush(enabledRule);
        ruleRepository.saveAndFlush(disabledRule);
        entityManager.clear();

        // When
        var rules = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created");

        // Then
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getRuleName()).isEqualTo("enabled-rule");
    }

    private RoutingRuleEntity createRule(String tenantId, String name, String sourceTopic, String targetTopic) {
        RoutingRuleEntity rule = new RoutingRuleEntity();
        rule.setTenantId(tenantId);
        rule.setRuleName(name);
        rule.setSourceTopic(sourceTopic);
        rule.setTargetTopic(targetTopic);
        rule.setPriority(Priority.MEDIUM);
        rule.setEnabled(true);
        return rule;
    }
}
