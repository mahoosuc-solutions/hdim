package com.healthdata.eventsourcing.command.observation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Implementation of ObservationEventEmitter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ObservationEventEmitterImpl implements ObservationEventEmitter {

    @Override
    public Mono<Void> emitObservationRecorded(ObservationRecordedEvent event) {
        return Mono.fromRunnable(() -> {
            log.info("Emitting ObservationRecordedEvent for patient: {}, LOINC: {}",
                event.getPatientId(), event.getLoincCode());
        });
    }
}
