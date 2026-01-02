package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.eventrouter.dto.RoutingResult;
import com.healthdata.eventrouter.entity.RoutingRuleEntity;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import com.healthdata.eventrouter.persistence.RoutingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Event Router Tests")
class EventRouterTest {

    @Mock
    private RoutingRuleRepository ruleRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private EventFilterService filterService;

    @Mock
    private EventTransformationService transformationService;

    @Mock
    private RouteMetricsService metricsService;

    private EventRouter eventRouter;

    @BeforeEach
    void setUp() {
        eventRouter = new EventRouter(
            ruleRepository,
            kafkaTemplate,
            filterService,
            transformationService,
            metricsService
        );
    }

    @Test
    @DisplayName("Should route event by type to correct topic")
    void shouldRouteEventByType() throws Exception {
        // Given
        EventMessage event = createEvent("PATIENT_CREATED", "tenant1", Map.of("resourceType", "Patient"));
        RoutingRuleEntity rule = createRule("patient-rule", "fhir.patient.created", "patient.processing", Priority.MEDIUM);

        when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created"))
            .thenReturn(Arrays.asList(rule));

        // When
        RoutingResult result = eventRouter.route(event);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTargetTopic()).isEqualTo("patient.processing");
        verify(kafkaTemplate).send(eq("patient.processing"), anyString());
        verify(metricsService).recordRoutedEvent(eq("patient.processing"), eq(Priority.MEDIUM));
    }

    @Test
    @DisplayName("Should route multiple events to different topics based on type")
    void shouldRouteMultipleEventTypes() throws Exception {
        // Given
        EventMessage patientEvent = createEvent("PATIENT_CREATED", "tenant1", Map.of("resourceType", "Patient"));
        EventMessage observationEvent = createEvent("OBSERVATION_CREATED", "tenant1", Map.of("resourceType", "Observation"));

        RoutingRuleEntity patientRule = createRule("patient-rule", "fhir.patient.created", "patient.processing", Priority.MEDIUM);
        RoutingRuleEntity obsRule = createRule("obs-rule", "fhir.observation.created", "observation.processing", Priority.HIGH);

        when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created"))
            .thenReturn(Arrays.asList(patientRule));
        when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.observation.created"))
            .thenReturn(Arrays.asList(obsRule));

        // When
        RoutingResult result1 = eventRouter.route(patientEvent);
        RoutingResult result2 = eventRouter.route(observationEvent);

        // Then
        assertThat(result1.getTargetTopic()).isEqualTo("patient.processing");
        assertThat(result2.getTargetTopic()).isEqualTo("observation.processing");
    }

    @Test
    @DisplayName("Should not route when no matching rule exists")
    void shouldNotRouteWithoutMatchingRule() {
        // Given
        EventMessage event = createEvent("UNKNOWN_EVENT", "tenant1", Map.of());
        when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue(anyString(), anyString()))
            .thenReturn(Arrays.asList());

        // When
        RoutingResult result = eventRouter.route(event);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReason()).contains("No matching routing rule");
        verify(kafkaTemplate, never()).send(anyString(), anyString());
        verify(metricsService).recordUnroutedEvent(anyString());
    }

    @Test
    @DisplayName("Should skip disabled rules")
    void shouldSkipDisabledRules() {
        // Given
        EventMessage event = createEvent("PATIENT_CREATED", "tenant1", Map.of());
        // Repository returns empty list because rule is disabled
        when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created"))
            .thenReturn(Arrays.asList());

        // When
        RoutingResult result = eventRouter.route(event);

        // Then
        assertThat(result.isSuccess()).isFalse();
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("Should filter events based on filter expression")
    void shouldFilterEventsBasedOnCriteria() {
        // Given
        EventMessage event = createEvent("PATIENT_CREATED", "tenant1", Map.of("status", "inactive"));
        RoutingRuleEntity rule = createRule("patient-rule", "fhir.patient.created", "patient.processing", Priority.MEDIUM);
        rule.setFilterExpression("{\"status\": \"active\"}");

        when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created"))
            .thenReturn(Arrays.asList(rule));
        when(filterService.matches(event, rule.getFilterExpression())).thenReturn(false);

        // When
        RoutingResult result = eventRouter.route(event);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReason()).contains("filtered out");
        verify(kafkaTemplate, never()).send(anyString(), anyString());
        verify(metricsService).recordFilteredEvent(anyString());
    }

    @Test
    @DisplayName("Should apply event transformation before routing")
    void shouldApplyTransformation() throws Exception {
        // Given
        EventMessage originalEvent = createEvent("PATIENT_CREATED", "tenant1", Map.of("name", "John"));

        RoutingRuleEntity rule = createRule("patient-rule", "fhir.patient.created", "patient.processing", Priority.MEDIUM);
        rule.setTransformationScript("enrichment-script");

        when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created"))
            .thenReturn(Arrays.asList(rule));

        // When
        RoutingResult result = eventRouter.route(originalEvent);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(transformationService).transform(any(), eq("enrichment-script"));
        verify(kafkaTemplate).send(eq("patient.processing"), anyString());
    }

    @Test
    @DisplayName("Should record metrics for routed events")
    void shouldRecordMetrics() throws Exception {
        // Given
        EventMessage event = createEvent("PATIENT_CREATED", "tenant1", Map.of());
        RoutingRuleEntity rule = createRule("patient-rule", "fhir.patient.created", "patient.processing", Priority.HIGH);

        when(ruleRepository.findByTenantIdAndSourceTopicAndEnabledTrue("tenant1", "fhir.patient.created"))
            .thenReturn(Arrays.asList(rule));

        // When
        eventRouter.route(event);

        // Then
        verify(metricsService).recordRoutedEvent("patient.processing", Priority.HIGH);
    }

    private EventMessage createEvent(String type, String tenantId, Map<String, Object> payload) {
        EventMessage event = new EventMessage();
        event.setEventType(type);
        event.setTenantId(tenantId);
        event.setSourceTopic("fhir." + type.toLowerCase().replace("_", "."));
        event.setPayload(payload);
        return event;
    }

    private RoutingRuleEntity createRule(String name, String sourceTopic, String targetTopic, Priority priority) {
        RoutingRuleEntity rule = new RoutingRuleEntity();
        rule.setRuleName(name);
        rule.setSourceTopic(sourceTopic);
        rule.setTargetTopic(targetTopic);
        rule.setPriority(priority);
        rule.setEnabled(true);
        rule.setTenantId("tenant1");
        return rule;
    }
}
