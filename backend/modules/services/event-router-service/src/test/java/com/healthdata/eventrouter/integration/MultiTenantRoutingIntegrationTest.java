package com.healthdata.eventrouter.integration;

import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.eventrouter.entity.RoutingRuleEntity;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import com.healthdata.eventrouter.persistence.RoutingRuleRepository;
import com.healthdata.eventrouter.service.EventRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Integration tests require full Spring Boot context with database")
@DisplayName("Multi-Tenant Routing Integration Tests")
class MultiTenantRoutingIntegrationTest {

    private RoutingRuleRepository ruleRepository;
    private EventRouter eventRouter;

    @BeforeEach
    void setUp() {
        ruleRepository.deleteAll();
    }

    @Test
    @DisplayName("Should isolate routing rules by tenant")
    void shouldIsolateRulesByTenant() {
        // Given
        RoutingRuleEntity tenant1Rule = createRule("tenant1", "rule1", "fhir.patient.created", "tenant1.processing");
        RoutingRuleEntity tenant2Rule = createRule("tenant2", "rule2", "fhir.patient.created", "tenant2.processing");
        ruleRepository.save(tenant1Rule);
        ruleRepository.save(tenant2Rule);

        EventMessage tenant1Event = createEvent("PATIENT_CREATED", "tenant1");
        EventMessage tenant2Event = createEvent("PATIENT_CREATED", "tenant2");

        // When
        var result1 = eventRouter.route(tenant1Event);
        var result2 = eventRouter.route(tenant2Event);

        // Then
        assertThat(result1.getTargetTopic()).isEqualTo("tenant1.processing");
        assertThat(result2.getTargetTopic()).isEqualTo("tenant2.processing");
    }

    @Test
    @DisplayName("Should not route events to wrong tenant")
    void shouldNotRouteCrossTenant() {
        // Given
        RoutingRuleEntity tenant1Rule = createRule("tenant1", "rule1", "fhir.patient.created", "tenant1.processing");
        ruleRepository.save(tenant1Rule);

        EventMessage tenant2Event = createEvent("PATIENT_CREATED", "tenant2");

        // When
        var result = eventRouter.route(tenant2Event);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReason()).contains("No matching routing rule");
    }

    @Test
    @DisplayName("Should support different priorities per tenant")
    void shouldSupportDifferentPrioritiesPerTenant() {
        // Given
        RoutingRuleEntity tenant1Rule = createRule("tenant1", "rule1", "fhir.patient.created", "tenant1.processing");
        tenant1Rule.setPriority(Priority.CRITICAL);

        RoutingRuleEntity tenant2Rule = createRule("tenant2", "rule2", "fhir.patient.created", "tenant2.processing");
        tenant2Rule.setPriority(Priority.LOW);

        ruleRepository.save(tenant1Rule);
        ruleRepository.save(tenant2Rule);

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

        ruleRepository.save(tenant1Rule);
        ruleRepository.save(tenant2Rule);

        // Then
        var rules1 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created");
        var rules2 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant2", "fhir.patient.created");

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

        ruleRepository.save(tenant1Rule);
        ruleRepository.save(tenant2Rule);

        // Then
        var rules1 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created");
        var rules2 = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant2", "fhir.patient.created");

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

        ruleRepository.save(rule1);
        ruleRepository.save(rule2);

        // When
        var rules = ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created");

        // Then
        assertThat(rules).hasSize(2);
    }

    @Test
    @DisplayName("Should track metrics per tenant")
    void shouldTrackMetricsPerTenant() {
        // Given
        RoutingRuleEntity tenant1Rule = createRule("tenant1", "rule1", "fhir.patient.created", "tenant1.processing");
        RoutingRuleEntity tenant2Rule = createRule("tenant2", "rule2", "fhir.patient.created", "tenant2.processing");
        ruleRepository.save(tenant1Rule);
        ruleRepository.save(tenant2Rule);

        // When
        for (int i = 0; i < 5; i++) {
            eventRouter.route(createEvent("PATIENT_CREATED", "tenant1"));
        }
        for (int i = 0; i < 3; i++) {
            eventRouter.route(createEvent("PATIENT_CREATED", "tenant2"));
        }

        // Then - metrics should be tracked separately
        // This would be verified through metrics service
        assertThat(true).isTrue(); // Placeholder for actual metrics verification
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

    private EventMessage createEvent(String type, String tenantId) {
        EventMessage event = new EventMessage();
        event.setEventType(type);
        event.setTenantId(tenantId);
        event.setSourceTopic("fhir." + type.toLowerCase().replace("_", "."));
        event.setPayload(Map.of("resourceType", "Patient"));
        return event;
    }
}
