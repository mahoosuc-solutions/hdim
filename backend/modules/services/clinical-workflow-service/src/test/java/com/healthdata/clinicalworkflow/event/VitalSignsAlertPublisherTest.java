package com.healthdata.clinicalworkflow.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VitalSignsAlertPublisher
 *
 * Tests issue #291 implementation: Kafka event publishing for abnormal vitals
 * with proper topic routing, partition keys, and event structure.
 */
@ExtendWith(MockitoExtension.class)
class VitalSignsAlertPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VitalSignsAlertPublisher publisher;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> partitionKeyCaptor;

    @Captor
    private ArgumentCaptor<String> eventJsonCaptor;

    private VitalSignsRecordEntity testVitals;
    private UUID patientId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        tenantId = "test-tenant";

        testVitals = VitalSignsRecordEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .patientId(patientId)
                .encounterId("encounter-123")
                .recordedBy("ma-smith")
                .systolicBp(new BigDecimal("180"))
                .diastolicBp(new BigDecimal("95"))
                .heartRate(new BigDecimal("102"))
                .temperatureF(new BigDecimal("98.6"))
                .respirationRate(new BigDecimal("18"))
                .oxygenSaturation(new BigDecimal("96"))
                .recordedAt(Instant.now())
                .alertStatus("critical")
                .alertMessage("Systolic BP 180 mmHg - critical high")
                .build();
    }

    @Test
    void shouldPublishCriticalAlertToCorrectTopic() throws Exception {
        // Given: Critical vital signs alert
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When: Publish alert
        publisher.publishAlert(testVitals, "Doe, John", "EXAM-101");

        // Then: Published to vitals.alert.critical topic
        verify(kafkaTemplate).send(
                topicCaptor.capture(),
                partitionKeyCaptor.capture(),
                eventJsonCaptor.capture()
        );

        assertThat(topicCaptor.getValue()).isEqualTo("vitals.alert.critical");
        assertThat(partitionKeyCaptor.getValue()).isEqualTo(patientId.toString());
    }

    @Test
    void shouldPublishWarningAlertToCorrectTopic() throws Exception {
        // Given: Warning vital signs alert
        testVitals.setAlertStatus("warning");
        testVitals.setAlertMessage("Systolic BP 145 mmHg - warning high");

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When: Publish alert
        publisher.publishAlert(testVitals, "Doe, John", "EXAM-101");

        // Then: Published to vitals.alert.warning topic
        verify(kafkaTemplate).send(
                topicCaptor.capture(),
                eq(patientId.toString()),
                anyString()
        );

        assertThat(topicCaptor.getValue()).isEqualTo("vitals.alert.warning");
    }

    @Test
    void shouldUsePatientIdAsPartitionKey() throws Exception {
        // Given: Vital signs alert
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When: Publish alert
        publisher.publishAlert(testVitals, null, null);

        // Then: Partition key is patient ID (ensures ordering per patient)
        verify(kafkaTemplate).send(
                anyString(),
                partitionKeyCaptor.capture(),
                anyString()
        );

        assertThat(partitionKeyCaptor.getValue()).isEqualTo(patientId.toString());
    }

    @Test
    void shouldIncludePatientNameWhenProvided() throws Exception {
        // Given: Alert with patient name
        String patientName = "Doe, John";
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any(VitalSignsAlertEvent.class)))
                .thenAnswer(invocation -> {
                    VitalSignsAlertEvent event = invocation.getArgument(0);
                    assertThat(event.getPatientName()).isEqualTo(patientName);
                    return "{}";
                });

        // When: Publish alert
        publisher.publishAlert(testVitals, patientName, null);

        // Then: Event includes patient name
        verify(objectMapper).writeValueAsString(any(VitalSignsAlertEvent.class));
    }

    @Test
    void shouldIncludeRoomNumberWhenProvided() throws Exception {
        // Given: Alert with room number
        String roomNumber = "EXAM-101";
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any(VitalSignsAlertEvent.class)))
                .thenAnswer(invocation -> {
                    VitalSignsAlertEvent event = invocation.getArgument(0);
                    assertThat(event.getRoomNumber()).isEqualTo(roomNumber);
                    return "{}";
                });

        // When: Publish alert
        publisher.publishAlert(testVitals, null, roomNumber);

        // Then: Event includes room number
        verify(objectMapper).writeValueAsString(any(VitalSignsAlertEvent.class));
    }

    @Test
    void shouldHandleNullPatientNameGracefully() throws Exception {
        // Given: Alert without patient name
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any(VitalSignsAlertEvent.class)))
                .thenAnswer(invocation -> {
                    VitalSignsAlertEvent event = invocation.getArgument(0);
                    assertThat(event.getPatientName()).isNull();
                    return "{}";
                });

        // When: Publish alert with null patient name
        publisher.publishAlert(testVitals, null, null);

        // Then: Event published successfully with null patient name
        verify(kafkaTemplate).send(anyString(), anyString(), anyString());
    }

    @Test
    void shouldHandleNullRoomNumberGracefully() throws Exception {
        // Given: Alert without room number
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any(VitalSignsAlertEvent.class)))
                .thenAnswer(invocation -> {
                    VitalSignsAlertEvent event = invocation.getArgument(0);
                    assertThat(event.getRoomNumber()).isNull();
                    return "{}";
                });

        // When: Publish alert with null room number
        publisher.publishAlert(testVitals, null, null);

        // Then: Event published successfully with null room number
        verify(kafkaTemplate).send(anyString(), anyString(), anyString());
    }

    @Test
    void shouldExtractAlertTypesFromMessage() throws Exception {
        // Given: Alert with high blood pressure
        testVitals.setAlertMessage("Systolic BP > 180 mmHg (critical)");
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any(VitalSignsAlertEvent.class)))
                .thenAnswer(invocation -> {
                    VitalSignsAlertEvent event = invocation.getArgument(0);
                    assertThat(event.getAlertTypes()).contains("HIGH_BLOOD_PRESSURE");
                    return "{}";
                });

        // When: Publish alert
        publisher.publishAlert(testVitals, null, null);

        // Then: Alert types extracted from message
        verify(objectMapper).writeValueAsString(any(VitalSignsAlertEvent.class));
    }

    @Test
    void shouldExtractMultipleAlertTypes() throws Exception {
        // Given: Alert with multiple abnormal vitals
        testVitals.setAlertMessage("Heart Rate > 130 bpm, O2 < 85%");
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any(VitalSignsAlertEvent.class)))
                .thenAnswer(invocation -> {
                    VitalSignsAlertEvent event = invocation.getArgument(0);
                    assertThat(event.getAlertTypes())
                            .containsExactlyInAnyOrder("HIGH_HEART_RATE", "LOW_OXYGEN_SATURATION");
                    return "{}";
                });

        // When: Publish alert
        publisher.publishAlert(testVitals, null, null);

        // Then: Multiple alert types extracted
        verify(objectMapper).writeValueAsString(any(VitalSignsAlertEvent.class));
    }

    @Test
    void shouldIncludeAllVitalSignValues() throws Exception {
        // Given: Alert with all vitals populated
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any(VitalSignsAlertEvent.class)))
                .thenAnswer(invocation -> {
                    VitalSignsAlertEvent event = invocation.getArgument(0);
                    VitalSignsAlertEvent.VitalSignsValues values = event.getValues();
                    assertThat(values.getSystolicBp()).isEqualByComparingTo(new BigDecimal("180"));
                    assertThat(values.getDiastolicBp()).isEqualByComparingTo(new BigDecimal("95"));
                    assertThat(values.getHeartRate()).isEqualByComparingTo(new BigDecimal("102"));
                    assertThat(values.getTemperatureF()).isEqualByComparingTo(new BigDecimal("98.6"));
                    assertThat(values.getRespirationRate()).isEqualByComparingTo(new BigDecimal("18"));
                    assertThat(values.getOxygenSaturation()).isEqualByComparingTo(new BigDecimal("96"));
                    return "{}";
                });

        // When: Publish alert
        publisher.publishAlert(testVitals, null, null);

        // Then: All vital signs included in event
        verify(objectMapper).writeValueAsString(any(VitalSignsAlertEvent.class));
    }

    @Test
    void shouldIncludeEventMetadata() throws Exception {
        // Given: Vital signs alert
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any(VitalSignsAlertEvent.class)))
                .thenAnswer(invocation -> {
                    VitalSignsAlertEvent event = invocation.getArgument(0);
                    assertThat(event.getEventId()).isNotNull();
                    assertThat(event.getEventType()).isEqualTo("VITAL_SIGNS_ALERT_CREATED");
                    assertThat(event.getEventSource()).isEqualTo("clinical-workflow-service");
                    assertThat(event.getEventTimestamp()).isNotNull();
                    return "{}";
                });

        // When: Publish alert
        publisher.publishAlert(testVitals, null, null);

        // Then: Event metadata included
        verify(objectMapper).writeValueAsString(any(VitalSignsAlertEvent.class));
    }

    @Test
    void shouldHandleJsonSerializationError() throws Exception {
        // Given: JSON serialization fails
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization error"));

        // When: Publish alert
        publisher.publishAlert(testVitals, null, null);

        // Then: Error logged but no exception thrown (non-blocking)
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void shouldHandleKafkaPublishError() throws Exception {
        // Given: Kafka publish fails
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When: Publish alert
        publisher.publishAlert(testVitals, null, null);

        // Then: Error logged but no exception thrown (non-blocking)
        verify(kafkaTemplate).send(anyString(), anyString(), anyString());
    }

    @Test
    void shouldNotThrowExceptionOnAnyFailure() {
        // Given: ObjectMapper is null (simulates unexpected error)
        publisher = new VitalSignsAlertPublisher(kafkaTemplate, null);

        // When: Publish alert
        publisher.publishAlert(testVitals, null, null);

        // Then: No exception thrown (non-blocking behavior)
        // Test passes if no exception is thrown
    }
}
