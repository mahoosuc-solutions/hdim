package com.healthdata.healthixadapter.hl7;

import com.healthdata.common.external.ExternalEventEnvelope;
import com.healthdata.common.external.ExternalEventMetadata;
import com.healthdata.common.external.PhiLevel;
import com.healthdata.common.external.SourceSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * HL7 v2.5 ADT message consumer.
 * Receives ADT (Admit/Discharge/Transfer) messages from Healthix hl7-service,
 * translates to HDIM domain events, and publishes to Kafka.
 *
 * Supports: A01 (Admit), A02 (Transfer), A03 (Discharge),
 * A04 (Register), A08 (Update), A28 (Add), A31 (Update)
 */
@Service
@ConditionalOnProperty(name = "external.healthix.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class Hl7AdtConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_HL7 = "external.healthix.hl7";
    private static final String TOPIC_PATIENTS = "external.healthix.patients";

    /**
     * Process an HL7 ADT message received from Healthix.
     */
    public void processAdtMessage(Map<String, Object> hl7Message, String tenantId) {
        String messageType = (String) hl7Message.getOrDefault("messageType", "ADT");
        String triggerEvent = (String) hl7Message.getOrDefault("triggerEvent", "A01");
        String patientId = (String) hl7Message.getOrDefault("patientId", "");

        log.info("Processing HL7 ADT message: type={}, trigger={}, patient={}",
                messageType, triggerEvent, patientId);

        ExternalEventEnvelope<Map<String, Object>> envelope = ExternalEventEnvelope.of(
                "external.healthix.hl7.adt." + triggerEvent.toLowerCase(),
                "healthix-adapter-service",
                tenantId,
                hl7Message,
                ExternalEventMetadata.builder()
                        .sourceSystem(SourceSystem.HEALTHIX)
                        .phiLevel(PhiLevel.FULL)
                        .build());

        kafkaTemplate.send(TOPIC_HL7, tenantId, envelope);

        // Also publish patient event for downstream processing
        if (isPatientEvent(triggerEvent)) {
            String patientEventType = mapToPatientEventType(triggerEvent);
            ExternalEventEnvelope<Map<String, Object>> patientEnvelope = ExternalEventEnvelope.of(
                    patientEventType,
                    "healthix-adapter-service",
                    tenantId,
                    hl7Message,
                    ExternalEventMetadata.builder()
                            .sourceSystem(SourceSystem.HEALTHIX)
                            .phiLevel(PhiLevel.FULL)
                            .build());
            kafkaTemplate.send(TOPIC_PATIENTS, tenantId, patientEnvelope);
        }
    }

    private boolean isPatientEvent(String triggerEvent) {
        return triggerEvent.matches("A0[1-4]|A08|A28|A31");
    }

    private String mapToPatientEventType(String triggerEvent) {
        return switch (triggerEvent) {
            case "A01" -> "fhir.encounter.created";
            case "A02" -> "fhir.encounter.updated";
            case "A03" -> "fhir.encounter.completed";
            case "A04", "A28" -> "fhir.patient.created";
            case "A08", "A31" -> "fhir.patient.updated";
            default -> "external.healthix.patients.event";
        };
    }
}
