package com.healthdata.healthixadapter.fhir;

import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("FhirSubscriptionClient")
class FhirSubscriptionClientTest {

    @Mock
    private RestTemplate fhirRestTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private AdapterSpanHelper spanHelper;

    private FhirSubscriptionClient client;

    @BeforeEach
    void setUp() {
        doAnswer(inv -> { ((Runnable) inv.getArgument(1)).run(); return null; })
                .when(spanHelper).tracedRun(anyString(), any(Runnable.class), any(String[].class));
        client = new FhirSubscriptionClient(fhirRestTemplate, kafkaTemplate, spanHelper);
    }

    @Test
    @DisplayName("should publish FHIR notification to Kafka with correct topic and envelope")
    void handleFhirNotification_shouldPublishToKafka() {
        Map<String, Object> fhirResource = new HashMap<>();
        fhirResource.put("resourceType", "Patient");
        fhirResource.put("id", "patient-123");

        client.handleFhirNotification(fhirResource, "tenant-1");

        var envelopeCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("external.healthix.fhir"), eq("tenant-1"), envelopeCaptor.capture());

        Object envelope = envelopeCaptor.getValue();
        assertThat(envelope).isNotNull();
        assertThat(envelope.toString()).contains("external.healthix.fhir.patient.received");
    }

    @Test
    @DisplayName("should handle unknown resource type in notification")
    void handleFhirNotification_shouldHandleUnknownResourceType() {
        Map<String, Object> fhirResource = new HashMap<>();

        client.handleFhirNotification(fhirResource, "tenant-1");

        verify(kafkaTemplate).send(eq("external.healthix.fhir"), eq("tenant-1"), any());
    }

    @Test
    @DisplayName("should register subscription with Healthix FHIR service")
    void registerSubscription_shouldPostToFhirEndpoint() {
        client.registerSubscription("https://hdim.example.com/fhir/callback", "Patient");

        var bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(fhirRestTemplate).postForEntity(eq("/fhir/Subscription"), bodyCaptor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) bodyCaptor.getValue();
        assertThat(body.get("resourceType")).isEqualTo("Subscription");
        assertThat(body.get("criteria")).isEqualTo("Patient?");
    }
}
