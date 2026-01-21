package com.healthdata.eventsourcing.command.patient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Implementation of PatientEventEmitter
 *
 * Handles event publication to Kafka and event store.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientEventEmitterImpl implements PatientEventEmitter {

    /**
     * Emit a PatientCreatedEvent to Kafka and event store
     *
     * @param event the event to emit
     * @return Mono that completes when event is published
     */
    @Override
    public Mono<Void> emitPatientCreated(PatientCreatedEvent event) {
        return Mono.fromRunnable(() -> {
            log.info("Emitting PatientCreatedEvent for aggregate: {}", event.getAggregateId());
            // Implementation would publish to Kafka and event store
            // For now, this is a stub
        });
    }
}
