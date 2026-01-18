package com.healthdata.eventsourcing.command.careplan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CarePlanEventEmitterImpl implements CarePlanEventEmitter {
    @Override
    public Mono<Void> emitCarePlanCreated(CarePlanCreatedEvent event) {
        return Mono.fromRunnable(() ->
            log.info("Emitting CarePlanCreatedEvent for patient: {}", event.getPatientId())
        );
    }
}
