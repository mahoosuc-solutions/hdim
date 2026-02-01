package com.healthdata.fhireventbridge.listener;

import com.healthdata.fhireventbridge.event.FhirPatientEvent;
import com.healthdata.fhireventbridge.service.PatientEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * FHIR Patient Event Listener
 *
 * Consumes FHIR Patient events and bridges them to domain event model.
 * Implements the Bridge pattern: isolates FHIR events from domain event models.
 *
 * ★ Insight ─────────────────────────────────────
 * - Decoupling: FHIR service publishes FHIR events, we convert to domain events
 * - Multi-identifier: Extracts FHIR Patient.identifier[] → PatientIdentifier list
 * - Patient merge: Processes Patient.link for merge chains (replaced-by link type)
 * - Idempotency: Uses aggregate ID for safe retries
 * - HIPAA compliance: Preserves sensitivity levels through conversion
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FhirPatientEventListener {

    private final PatientEventPublisher patientEventPublisher;

    /**
     * Consume FHIR Patient.created events
     * Convert to domain PatientCreatedEvent and publish
     *
     * @param fhirEvent FHIR patient creation event from fhir-service
     * @param partition Kafka partition
     * @param offset Kafka offset
     * @param ack Manual acknowledgment handler
     */
    @KafkaListener(topics = "fhir.patient.created", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeFhirPatientCreated(
            @Payload FhirPatientEvent fhirEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        try {
            log.info("Received FHIR patient.created event: fhirResourceId={}, tenant={}, partition={}, offset={}",
                fhirEvent.getFhirResourceId(), fhirEvent.getTenantId(), partition, offset);

            // Publish converted domain event
            patientEventPublisher.publishPatientCreated(fhirEvent);

            ack.acknowledge();
            log.debug("Successfully processed FHIR patient.created event: fhirResourceId={}",
                fhirEvent.getFhirResourceId());

        } catch (Exception e) {
            log.error("Error processing FHIR patient.created event: fhirResourceId={}",
                fhirEvent.getFhirResourceId(), e);
            // Do not acknowledge - allow retry
            throw new RuntimeException("Failed to process FHIR patient.created event", e);
        }
    }

    /**
     * Consume FHIR Patient.updated events
     * Detects identifier changes and publishes appropriate domain events
     *
     * @param fhirEvent FHIR patient update event
     * @param ack Manual acknowledgment handler
     */
    @KafkaListener(topics = "fhir.patient.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeFhirPatientUpdated(
            @Payload FhirPatientEvent fhirEvent,
            Acknowledgment ack) {

        try {
            log.info("Received FHIR patient.updated event: fhirResourceId={}, tenant={}",
                fhirEvent.getFhirResourceId(), fhirEvent.getTenantId());

            // Analyze what changed and publish appropriate events
            patientEventPublisher.publishPatientUpdated(fhirEvent);

            ack.acknowledge();
            log.debug("Successfully processed FHIR patient.updated event: fhirResourceId={}",
                fhirEvent.getFhirResourceId());

        } catch (Exception e) {
            log.error("Error processing FHIR patient.updated event: fhirResourceId={}",
                fhirEvent.getFhirResourceId(), e);
            throw new RuntimeException("Failed to process FHIR patient.updated event", e);
        }
    }

    /**
     * Consume FHIR Patient.linked events
     * Processes Patient.link (merge chains, related records)
     *
     * @param fhirEvent FHIR patient link event
     * @param ack Manual acknowledgment handler
     */
    @KafkaListener(topics = "fhir.patient.linked", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeFhirPatientLinked(
            @Payload FhirPatientEvent fhirEvent,
            Acknowledgment ack) {

        try {
            log.info("Received FHIR patient.linked event: fhirResourceId={}, linkedFhirResourceId={}, linkType={}",
                fhirEvent.getFhirResourceId(), fhirEvent.getLinkedFhirResourceId(), fhirEvent.getLinkType());

            // Process merge chains, update projections, cascade updates
            patientEventPublisher.publishPatientLinked(fhirEvent);

            ack.acknowledge();
            log.debug("Successfully processed FHIR patient.linked event: fhirResourceId={}",
                fhirEvent.getFhirResourceId());

        } catch (Exception e) {
            log.error("Error processing FHIR patient.linked event: fhirResourceId={}",
                fhirEvent.getFhirResourceId(), e);
            throw new RuntimeException("Failed to process FHIR patient.linked event", e);
        }
    }
}
