package com.healthdata.eventsourcing.command.observation;

import reactor.core.publisher.Mono;

/**
 * Interface for emitting observation-related domain events.
 */
public interface ObservationEventEmitter {

    /**
     * Emit an ObservationRecordedEvent
     *
     * @param event the event to emit
     * @return Mono that completes when event is published
     */
    Mono<Void> emitObservationRecorded(ObservationRecordedEvent event);
}
