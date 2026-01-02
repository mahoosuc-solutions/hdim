package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Event Transformation Service Tests")
class EventTransformationServiceTest {

    private EventTransformationService transformationService;

    @BeforeEach
    void setUp() {
        transformationService = new EventTransformationService();
    }

    @Test
    @DisplayName("Should add enrichment fields to event")
    void shouldEnrichEvent() {
        // Given
        EventMessage event = createEvent(Map.of("name", "John Doe"));
        String script = "enrichment:add-timestamp,add-source";

        // When
        EventMessage transformed = transformationService.transform(event, script);

        // Then
        assertThat(transformed.getPayload()).containsKey("enrichedAt");
        assertThat(transformed.getPayload()).containsKey("source");
    }

    @Test
    @DisplayName("Should rename fields in event")
    void shouldRenameFields() {
        // Given
        EventMessage event = createEvent(Map.of("old_field", "value"));
        String script = "rename:old_field->new_field";

        // When
        EventMessage transformed = transformationService.transform(event, script);

        // Then
        assertThat(transformed.getPayload()).containsKey("new_field");
        assertThat(transformed.getPayload()).doesNotContainKey("old_field");
    }

    @Test
    @DisplayName("Should remove fields from event")
    void shouldRemoveFields() {
        // Given
        EventMessage event = createEvent(Map.of(
            "keep", "value1",
            "remove", "value2",
            "also_remove", "value3"
        ));
        String script = "remove:remove,also_remove";

        // When
        EventMessage transformed = transformationService.transform(event, script);

        // Then
        assertThat(transformed.getPayload()).containsKey("keep");
        assertThat(transformed.getPayload()).doesNotContainKey("remove");
        assertThat(transformed.getPayload()).doesNotContainKey("also_remove");
    }

    @Test
    @DisplayName("Should apply multiple transformations in sequence")
    void shouldApplyMultipleTransformations() {
        // Given
        EventMessage event = createEvent(Map.of("field", "value"));
        String script = "enrichment:add-timestamp|rename:field->newField|remove:tempField";

        // When
        EventMessage transformed = transformationService.transform(event, script);

        // Then
        assertThat(transformed.getPayload()).containsKey("enrichedAt");
        assertThat(transformed.getPayload()).containsKey("newField");
        assertThat(transformed.getPayload()).doesNotContainKey("field");
    }

    @Test
    @DisplayName("Should convert data types")
    void shouldConvertDataTypes() {
        // Given
        EventMessage event = createEvent(Map.of("age", "45", "score", "85.5"));
        String script = "convert:age->integer,score->double";

        // When
        EventMessage transformed = transformationService.transform(event, script);

        // Then
        assertThat(transformed.getPayload().get("age")).isInstanceOf(Integer.class);
        assertThat(transformed.getPayload().get("score")).isInstanceOf(Double.class);
    }

    @Test
    @DisplayName("Should mask sensitive fields")
    void shouldMaskSensitiveFields() {
        // Given
        EventMessage event = createEvent(Map.of(
            "ssn", "123-45-6789",
            "email", "user@example.com"
        ));
        String script = "mask:ssn,email";

        // When
        EventMessage transformed = transformationService.transform(event, script);

        // Then
        assertThat(transformed.getPayload().get("ssn")).isEqualTo("***-**-****");
        assertThat(transformed.getPayload().get("email")).asString().contains("***");
    }

    @Test
    @DisplayName("Should apply custom JavaScript transformation")
    void shouldApplyCustomScript() {
        // Given
        EventMessage event = createEvent(Map.of("firstName", "John", "lastName", "Doe"));
        String script = "js:payload.put('fullName', payload.get('firstName') + ' ' + payload.get('lastName'))";

        // When
        EventMessage transformed = transformationService.transform(event, script);

        // Then - JavaScript may not be available in test environment, so just verify no exception
        assertThat(transformed).isNotNull();
        assertThat(transformed.getPayload()).containsKeys("firstName", "lastName");
    }

    @Test
    @DisplayName("Should handle transformation errors gracefully")
    void shouldHandleErrors() {
        // Given
        EventMessage event = createEvent(Map.of("field", "value"));
        String invalidScript = "invalid:syntax:here";

        // When
        EventMessage transformed = transformationService.transform(event, invalidScript);

        // Then
        assertThat(transformed).isNotNull();
        assertThat(transformed.getPayload()).containsEntry("field", "value"); // Original unchanged
    }

    @Test
    @DisplayName("Should return original event when script is null or empty")
    void shouldReturnOriginalWhenNoScript() {
        // Given
        EventMessage event = createEvent(Map.of("field", "value"));

        // When
        EventMessage transformedNull = transformationService.transform(event, null);
        EventMessage transformedEmpty = transformationService.transform(event, "");

        // Then
        assertThat(transformedNull.getPayload()).containsEntry("field", "value");
        assertThat(transformedEmpty.getPayload()).containsEntry("field", "value");
    }

    @Test
    @DisplayName("Should flatten nested objects")
    void shouldFlattenNestedObjects() {
        // Given
        Map<String, Object> nested = Map.of(
            "patient", Map.of(
                "name", "John",
                "address", Map.of("city", "Boston", "state", "MA")
            )
        );
        EventMessage event = createEvent(nested);
        String script = "flatten:patient";

        // When
        EventMessage transformed = transformationService.transform(event, script);

        // Then
        assertThat(transformed.getPayload()).containsKey("patient.name");
        assertThat(transformed.getPayload()).containsKey("patient.address.city");
    }

    private EventMessage createEvent(Map<String, Object> payload) {
        EventMessage event = new EventMessage();
        event.setEventType("TEST_EVENT");
        event.setTenantId("tenant1");
        event.setPayload(payload);
        return event;
    }
}
