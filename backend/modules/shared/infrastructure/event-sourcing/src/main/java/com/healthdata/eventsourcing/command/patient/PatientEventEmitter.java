package com.healthdata.eventsourcing.command.patient;

import reactor.core.publisher.Mono;

/**
 * Interface for emitting patient-related domain events.
 *
 * Handles event publication to:
 * - Event store (for audit)
 * - Kafka (for projection updates)
 * - Downstream services (via Kafka topics)
 */
public interface PatientEventEmitter {

    /**
     * Emit a PatientCreatedEvent
     *
     * @param event the event to emit
     * @return Mono that completes when event is published
     */
    Mono<Void> emitPatientCreated(PatientCreatedEvent event);
}
