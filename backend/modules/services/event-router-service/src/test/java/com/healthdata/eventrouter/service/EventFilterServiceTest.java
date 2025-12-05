package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Event Filter Service Tests")
class EventFilterServiceTest {

    private EventFilterService filterService;

    @BeforeEach
    void setUp() {
        filterService = new EventFilterService();
    }

    @Test
    @DisplayName("Should match event with simple equality filter")
    void shouldMatchSimpleEquality() {
        // Given
        EventMessage event = createEvent(Map.of("status", "active", "type", "Patient"));
        String filterExpression = "{\"status\": \"active\"}";

        // When
        boolean matches = filterService.matches(event, filterExpression);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should not match when field value differs")
    void shouldNotMatchWhenValueDiffers() {
        // Given
        EventMessage event = createEvent(Map.of("status", "inactive"));
        String filterExpression = "{\"status\": \"active\"}";

        // When
        boolean matches = filterService.matches(event, filterExpression);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should match multiple field conditions (AND logic)")
    void shouldMatchMultipleFields() {
        // Given
        EventMessage event = createEvent(Map.of(
            "status", "active",
            "type", "Patient",
            "verified", true
        ));
        String filterExpression = "{\"status\": \"active\", \"type\": \"Patient\"}";

        // When
        boolean matches = filterService.matches(event, filterExpression);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should not match when one field doesn't match (AND logic)")
    void shouldNotMatchWhenOneFieldFails() {
        // Given
        EventMessage event = createEvent(Map.of(
            "status", "active",
            "type", "Observation"
        ));
        String filterExpression = "{\"status\": \"active\", \"type\": \"Patient\"}";

        // When
        boolean matches = filterService.matches(event, filterExpression);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should support nested field matching")
    void shouldMatchNestedFields() {
        // Given
        Map<String, Object> address = Map.of("city", "Boston", "state", "MA");
        EventMessage event = createEvent(Map.of("patient", Map.of("address", address)));
        String filterExpression = "{\"patient.address.city\": \"Boston\"}";

        // When
        boolean matches = filterService.matches(event, filterExpression);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should support numeric comparisons")
    void shouldSupportNumericComparisons() {
        // Given
        EventMessage event = createEvent(Map.of("age", 45, "score", 85.5));
        String filterExpression = "{\"age\": {\"$gte\": 18}, \"score\": {\"$gt\": 80}}";

        // When
        boolean matches = filterService.matches(event, filterExpression);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should support IN operator")
    void shouldSupportInOperator() {
        // Given
        EventMessage event = createEvent(Map.of("status", "pending"));
        String filterExpression = "{\"status\": {\"$in\": [\"pending\", \"active\", \"completed\"]}}";

        // When
        boolean matches = filterService.matches(event, filterExpression);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should support NOT operator")
    void shouldSupportNotOperator() {
        // Given
        EventMessage event = createEvent(Map.of("status", "active"));
        String filterExpression = "{\"status\": {\"$ne\": \"deleted\"}}";

        // When
        boolean matches = filterService.matches(event, filterExpression);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should support EXISTS operator")
    void shouldSupportExistsOperator() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "test@example.com");
        EventMessage event = createEvent(payload);
        String filterExpression = "{\"email\": {\"$exists\": true}}";

        // When
        boolean matches = filterService.matches(event, filterExpression);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should support regex pattern matching")
    void shouldSupportRegexMatching() {
        // Given
        EventMessage event = createEvent(Map.of("email", "user@healthdata.com"));
        String filterExpression = "{\"email\": {\"$regex\": \".*@healthdata\\\\.com$\"}}";

        // When
        boolean matches = filterService.matches(event, filterExpression);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should match all events when filter is empty or null")
    void shouldMatchAllWhenFilterEmpty() {
        // Given
        EventMessage event = createEvent(Map.of("any", "value"));

        // When
        boolean matchesNull = filterService.matches(event, null);
        boolean matchesEmpty = filterService.matches(event, "");
        boolean matchesEmptyJson = filterService.matches(event, "{}");

        // Then
        assertThat(matchesNull).isTrue();
        assertThat(matchesEmpty).isTrue();
        assertThat(matchesEmptyJson).isTrue();
    }

    @Test
    @DisplayName("Should handle filter expression parsing errors gracefully")
    void shouldHandleParsingErrors() {
        // Given
        EventMessage event = createEvent(Map.of("field", "value"));
        String invalidFilter = "{invalid json";

        // When
        boolean matches = filterService.matches(event, invalidFilter);

        // Then
        assertThat(matches).isFalse(); // Should not match on error
    }

    private EventMessage createEvent(Map<String, Object> payload) {
        EventMessage event = new EventMessage();
        event.setEventType("TEST_EVENT");
        event.setTenantId("tenant1");
        event.setPayload(new HashMap<>(payload));
        return event;
    }
}
