package com.healthdata.healthixadapter.service;

import com.healthdata.healthixadapter.hl7.Hl7AdtConsumer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class Hl7AdtConsumerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private Hl7AdtConsumer hl7AdtConsumer;

    @Test
    void processAdtMessage_shouldPublishToHl7Topic() {
        Map<String, Object> hl7Message = Map.of(
                "messageType", "ADT",
                "triggerEvent", "A01",
                "patientId", "patient-123"
        );

        hl7AdtConsumer.processAdtMessage(hl7Message, "tenant-1");

        verify(kafkaTemplate).send(eq("external.healthix.hl7"), eq("tenant-1"), any());
    }

    @Test
    void processAdtMessage_A01_shouldAlsoPublishPatientEvent() {
        Map<String, Object> hl7Message = Map.of(
                "messageType", "ADT",
                "triggerEvent", "A01",
                "patientId", "patient-123"
        );

        hl7AdtConsumer.processAdtMessage(hl7Message, "tenant-1");

        // Should publish to both HL7 topic and patients topic
        verify(kafkaTemplate, times(2)).send(any(String.class), eq("tenant-1"), any());
        verify(kafkaTemplate).send(eq("external.healthix.patients"), eq("tenant-1"), any());
    }

    @Test
    void processAdtMessage_A28_shouldMapToPatientCreated() {
        Map<String, Object> hl7Message = Map.of(
                "messageType", "ADT",
                "triggerEvent", "A28",
                "patientId", "new-patient"
        );

        hl7AdtConsumer.processAdtMessage(hl7Message, "tenant-1");

        ArgumentCaptor<Object> envelopeCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("external.healthix.patients"), eq("tenant-1"), envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue()).isNotNull();
    }

    @Test
    void processAdtMessage_A02Transfer_shouldPublishEncounterUpdated() {
        Map<String, Object> message = Map.of(
                "messageType", "ADT",
                "triggerEvent", "A02",
                "patientId", "patient-transfer");

        hl7AdtConsumer.processAdtMessage(message, "tenant-1");

        verify(kafkaTemplate).send(eq("external.healthix.hl7"), eq("tenant-1"), any());
        ArgumentCaptor<Object> patientCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("external.healthix.patients"), eq("tenant-1"), patientCaptor.capture());
        assertThat(patientCaptor.getValue().toString()).contains("fhir.encounter.updated");
    }

    @Test
    void processAdtMessage_A03Discharge_shouldPublishEncounterCompleted() {
        Map<String, Object> message = Map.of(
                "messageType", "ADT",
                "triggerEvent", "A03",
                "patientId", "patient-discharge");

        hl7AdtConsumer.processAdtMessage(message, "tenant-1");

        verify(kafkaTemplate, times(2)).send(any(String.class), eq("tenant-1"), any());
        ArgumentCaptor<Object> patientCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("external.healthix.patients"), eq("tenant-1"), patientCaptor.capture());
        assertThat(patientCaptor.getValue().toString()).contains("fhir.encounter.completed");
    }

    @Test
    void processAdtMessage_A04Register_shouldPublishPatientCreated() {
        Map<String, Object> message = Map.of(
                "messageType", "ADT",
                "triggerEvent", "A04",
                "patientId", "patient-register");

        hl7AdtConsumer.processAdtMessage(message, "tenant-1");

        ArgumentCaptor<Object> patientCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("external.healthix.patients"), eq("tenant-1"), patientCaptor.capture());
        assertThat(patientCaptor.getValue().toString()).contains("fhir.patient.created");
    }

    @Test
    void processAdtMessage_A08Update_shouldPublishPatientUpdated() {
        Map<String, Object> message = Map.of(
                "messageType", "ADT",
                "triggerEvent", "A08",
                "patientId", "patient-update");

        hl7AdtConsumer.processAdtMessage(message, "tenant-1");

        ArgumentCaptor<Object> patientCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("external.healthix.patients"), eq("tenant-1"), patientCaptor.capture());
        assertThat(patientCaptor.getValue().toString()).contains("fhir.patient.updated");
    }

    @Test
    void processAdtMessage_A31Update_shouldPublishPatientUpdated() {
        Map<String, Object> message = Map.of(
                "messageType", "ADT",
                "triggerEvent", "A31",
                "patientId", "patient-a31-update");

        hl7AdtConsumer.processAdtMessage(message, "tenant-1");

        verify(kafkaTemplate).send(eq("external.healthix.patients"), eq("tenant-1"), any());
    }

    @Test
    void processAdtMessage_A05NonPatientEvent_shouldOnlyPublishToHl7Topic() {
        Map<String, Object> message = Map.of(
                "messageType", "ADT",
                "triggerEvent", "A05",
                "patientId", "patient-other");

        hl7AdtConsumer.processAdtMessage(message, "tenant-1");

        verify(kafkaTemplate, times(1)).send(any(String.class), any(String.class), any());
        verify(kafkaTemplate).send(eq("external.healthix.hl7"), eq("tenant-1"), any());
    }
}
