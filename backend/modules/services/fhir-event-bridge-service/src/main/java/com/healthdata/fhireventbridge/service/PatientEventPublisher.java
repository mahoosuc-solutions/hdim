package com.healthdata.fhireventbridge.service;

import com.healthdata.fhireventbridge.event.FhirPatientEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes domain patient events converted from FHIR events.
 * Bridges FHIR event model to domain event model via Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPatientCreated(FhirPatientEvent fhirEvent) {
        log.info("Publishing patient.created domain event for fhirResourceId={}, tenant={}",
            fhirEvent.getFhirResourceId(), fhirEvent.getTenantId());
        kafkaTemplate.send("patient.created", fhirEvent.getFhirResourceId(), fhirEvent);
    }

    public void publishPatientUpdated(FhirPatientEvent fhirEvent) {
        log.info("Publishing patient.updated domain event for fhirResourceId={}, tenant={}",
            fhirEvent.getFhirResourceId(), fhirEvent.getTenantId());
        kafkaTemplate.send("patient.identifier.changed", fhirEvent.getFhirResourceId(), fhirEvent);
    }

    public void publishPatientLinked(FhirPatientEvent fhirEvent) {
        log.info("Publishing patient.linked domain event for fhirResourceId={}, linkedFhirResourceId={}, tenant={}",
            fhirEvent.getFhirResourceId(), fhirEvent.getLinkedFhirResourceId(), fhirEvent.getTenantId());
        kafkaTemplate.send("patient.linked", fhirEvent.getFhirResourceId(), fhirEvent);
    }
}
